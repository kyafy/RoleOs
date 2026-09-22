package io.roleos.browser.boss;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.browser.port.BrowserProvider;
import io.roleos.browser.port.BrowserProvider.BrowserProviderException;
import io.roleos.browser.port.BrowserProvider.FailureCategory;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.port.JobSourcePort.RawJobDetail;
import io.roleos.job.port.JobSourcePort.RawJobSummary;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 解析 Kimi 公开 accessibility 树；只保留岗位事实，排除账户导航及 Provider 引用。 */
final class BossAccessibilityParser {
  private static final String ROLE_HEADING = "heading";
  private static final String ROLE_LINK = "link";
  private static final String ROLE_LIST_ITEM = "listitem";
  private static final String ROLE_PARAGRAPH = "paragraph";
  private static final String ROLE_STATIC_TEXT = "StaticText";
  private static final String NODE_CHILDREN = "children";
  private static final String NODE_NAME = "name";
  private static final String NODE_ROLE = "role";

  private final ObjectMapper mapper = new ObjectMapper();

  List<RawJobSummary> list(String text) {
    JsonNode root = read(text);
    if (root.path("cards").isArray() && !root.path("cards").isEmpty()) {
      return cards(root.path("cards"));
    }
    List<RawJobSummary> result = new ArrayList<>();
    collectCards(root.path("tree"), root.path("links"), result);
    if (result.isEmpty()) throw failure("BOSS_LIST_EMPTY_OR_CHANGED");
    return List.copyOf(result);
  }

  String detail(String text) {
    JsonNode root = read(text);
    List<JsonNode> nodes = new ArrayList<>();
    flatten(root.path("tree"), nodes);
    boolean inDescription = false;
    List<String> paragraphs = new ArrayList<>();
    for (JsonNode node : nodes) {
      String role = node.path(NODE_ROLE).asText();
      if (ROLE_HEADING.equals(role)) {
        if (inDescription) break;
        inDescription = "职位描述".equals(node.path(NODE_NAME).asText());
      } else if (inDescription && ROLE_PARAGRAPH.equals(role)) {
        String paragraph = visibleText(node);
        if (!paragraph.isBlank()) paragraphs.add(paragraph);
      }
    }
    if (paragraphs.isEmpty()) throw failure("BOSS_DETAIL_STRUCTURE_CHANGED");
    return String.join("\n", paragraphs);
  }

  /** 只消费 Kimi 从可见岗位卡片提取的公开事实。卡片没有公司名时保留为空，必须由详情页补齐， 不能把招聘者名称或页面位置误写为公司事实。 */
  List<RawJobSummary> cards(JsonNode cards) {
    List<RawJobSummary> result = new ArrayList<>();
    for (JsonNode card : cards) {
      URI url = trustedJobUrl(card.path("url").asText());
      String path = url.getPath();
      String id = path.substring("/job_detail/".length(), path.length() - ".html".length());
      List<String> tags = new ArrayList<>();
      card.path("tags").forEach(tag -> tags.add(tag.asText()));
      String location = nullable(card.path("location").asText());
      result.add(
          new RawJobSummary(
              Source.BOSS,
              id,
              required(card, "title"),
              null,
              city(location),
              nullable(card.path("salary").asText()),
              matching(tags, ".*(年|经验不限|在校/应届)$"),
              matching(tags, "(博士|硕士|本科|大专|高中|中专|初中及以下|学历不限)"),
              null,
              RecruiterActivity.UNKNOWN,
              url,
              Map.of("visibleJobFacts", List.copyOf(tags))));
    }
    if (result.isEmpty()) throw failure("BOSS_LIST_EMPTY_OR_CHANGED");
    return List.copyOf(result);
  }

  /** 详情页是公司与 JD 的权威来源；缺任一事实时安全失败而非构造占位值。 */
  RawJobDetail detailFacts(String text, RawJobSummary summary) {
    JsonNode detail = read(text).path("detail");
    String company = required(detail, "company");
    String jd = required(detail, "jd");
    RawJobSummary enriched =
        new RawJobSummary(
            summary.source(),
            summary.externalJobId(),
            summary.title(),
            company,
            summary.city(),
            summary.salaryText(),
            summary.experienceText(),
            summary.educationText(),
            summary.publishTime(),
            summary.recruiterActivity(),
            summary.sourceUrl(),
            summary.rawData());
    return new RawJobDetail(
        enriched,
        jd,
        Map.of("jd", jd, "company", company, "listFacts", summary.rawData()),
        java.time.Instant.now(),
        io.roleos.job.domain.JobTypes.BrowserProviderType.KIMI_WEBBRIDGE);
  }

  private void collectCards(JsonNode nodes, JsonNode links, List<RawJobSummary> result) {
    for (JsonNode node : nodes) {
      List<JsonNode> directLinks = new ArrayList<>();
      for (JsonNode child : node.path(NODE_CHILDREN)) {
        if (ROLE_LINK.equals(child.path(NODE_ROLE).asText())) directLinks.add(child);
      }
      if (ROLE_LIST_ITEM.equals(node.path(NODE_ROLE).asText()) && directLinks.size() == 2) {
        String title = directLinks.getFirst().path(NODE_NAME).asText();
        String company = directLinks.getLast().path(NODE_NAME).asText();
        URI url = jobUrl(title, links);
        if (url != null && !company.isBlank()) {
          List<String> facts = new ArrayList<>();
          collectText(node, facts);
          String city = null;
          String salary = null;
          for (JsonNode child : node.path(NODE_CHILDREN)) {
            if (!ROLE_STATIC_TEXT.equals(child.path(NODE_ROLE).asText())) continue;
            String value = child.path(NODE_NAME).asText();
            if (value.contains("K") || value.contains("万")) salary = value;
            else if (!value.isBlank()) city = value.split("·", 2)[0];
          }
          String path = url.getPath();
          String id = path.substring("/job_detail/".length(), path.length() - ".html".length());
          result.add(
              new RawJobSummary(
                  Source.BOSS,
                  id,
                  title,
                  company,
                  city,
                  salary,
                  matching(facts, ".*(年|经验不限|在校/应届)$"),
                  matching(facts, "(博士|硕士|本科|大专|高中|中专|初中及以下|学历不限)"),
                  null,
                  RecruiterActivity.UNKNOWN,
                  url,
                  Map.of("visibleJobFacts", List.copyOf(facts))));
          continue;
        }
      }
      collectCards(node.path("children"), links, result);
    }
  }

  private static URI jobUrl(String title, JsonNode links) {
    List<URI> matches = new ArrayList<>();
    for (JsonNode link : links) {
      if (!title.equals(link.path(NODE_NAME).asText())) continue;
      URI url = URI.create(link.path("url").asText());
      BrowserProvider.requireTrustedUrl(url);
      if (url.getPath().matches("/job_detail/[A-Za-z0-9_~-]+\\.html")) matches.add(url);
    }
    // 同名岗位多链接时不能将职位事实错误地关联到另一来源身份。
    return matches.stream().distinct().count() == 1 ? matches.getFirst() : null;
  }

  private static URI trustedJobUrl(String value) {
    try {
      URI url = URI.create(value);
      BrowserProvider.requireTrustedUrl(url);
      if (!url.getPath().matches("/job_detail/[A-Za-z0-9_~-]+\\.html"))
        throw failure("BOSS_CARD_URL_INVALID");
      return url;
    } catch (IllegalArgumentException exception) {
      throw failure("BOSS_CARD_URL_INVALID", exception);
    }
  }

  private static String required(JsonNode node, String field) {
    String value = nullable(node.path(field).asText());
    if (value == null) throw failure("BOSS_DETAIL_STRUCTURE_CHANGED");
    return value;
  }

  private static String nullable(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static String city(String location) {
    return location == null ? null : location.split("·", 2)[0];
  }

  private static String matching(List<String> facts, String pattern) {
    return facts.stream().filter(value -> value.matches(pattern)).findFirst().orElse(null);
  }

  private static String visibleText(JsonNode node) {
    List<String> text = new ArrayList<>();
    collectText(node, text);
    return String.join("", text);
  }

  private static void collectText(JsonNode node, List<String> text) {
    if (ROLE_STATIC_TEXT.equals(node.path(NODE_ROLE).asText())) {
      text.add(node.path(NODE_NAME).asText());
      return;
    }
    for (JsonNode child : node.path(NODE_CHILDREN)) collectText(child, text);
  }

  private static void flatten(JsonNode nodes, List<JsonNode> result) {
    for (JsonNode node : nodes) {
      result.add(node);
      flatten(node.path(NODE_CHILDREN), result);
    }
  }

  private JsonNode read(String text) {
    try {
      JsonNode root = mapper.readTree(text);
      if (!root.isObject() || !root.path("tree").isArray()) throw failure("BOSS_SNAPSHOT_INVALID");
      return root;
    } catch (JsonProcessingException exception) {
      throw failure("BOSS_SNAPSHOT_INVALID", exception);
    }
  }

  private static BrowserProviderException failure(String code) {
    return new BrowserProviderException(FailureCategory.TERMINAL_FAILURE, code, "Boss 页面结构无法安全解析");
  }

  private static BrowserProviderException failure(String code, Exception cause) {
    return new BrowserProviderException(
        FailureCategory.TERMINAL_FAILURE, code, "Boss 页面结构无法安全解析", cause);
  }
}
