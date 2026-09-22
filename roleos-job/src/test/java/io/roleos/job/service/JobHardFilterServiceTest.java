package io.roleos.job.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.domain.JobTypes.SourceStatus;
import io.roleos.job.service.JobHardFilterService.FilterCriteria;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** 20 条确定性 Fixture 锁定四类规则与 Reject/Unknown/Pass 汇总语义。 */
class JobHardFilterServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-19T06:00:00Z");
  private final JobHardFilterService service =
      new JobHardFilterService(Clock.fixed(NOW, ZoneOffset.UTC), "hard-filter-v1");

  @ParameterizedTest(name = "{0}")
  @MethodSource("fixtures")
  void evaluatesDeterministically(
      String name, Job job, FilterCriteria criteria, FilterResult expected) {
    var evaluation = service.evaluate(UUID.randomUUID(), job, criteria);

    assertThat(evaluation.overallResult()).isEqualTo(expected);
    assertThat(evaluation.city().reasonCode()).isNotBlank();
    assertThat(evaluation.salary().reasonCode()).isNotBlank();
    assertThat(evaluation.experience().reasonCode()).isNotBlank();
    assertThat(evaluation.targetRole().reasonCode()).isNotBlank();
  }

  private static Stream<Arguments> fixtures() {
    FilterCriteria baseline = new FilterCriteria("上海", 20_000, 5, "AI Agent Engineer");
    return Stream.of(
        args(
            "all pass",
            job("上海", 25_000, 35_000, 3, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.PASS),
        args(
            "city reject",
            job("北京", 25_000, 35_000, 3, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.REJECT),
        args(
            "city unknown",
            job(null, 25_000, 35_000, 3, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.UNKNOWN),
        args(
            "remote pass",
            job("Remote", 25_000, 35_000, 3, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.PASS),
        args(
            "multiple cities pass",
            job("上海/北京", 25_000, 35_000, 3, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.PASS),
        args(
            "salary reject",
            job("上海", 10_000, 15_000, 3, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.REJECT),
        args(
            "salary boundary pass",
            job("上海", 15_000, 20_000, 3, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.PASS),
        args(
            "salary unknown",
            job("上海", null, null, 3, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.UNKNOWN),
        args(
            "salary partial known pass",
            job("上海", 25_000, null, 3, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.PASS),
        args(
            "experience reject",
            job("上海", 25_000, 35_000, 6, 8, "AI Agent Engineer"),
            baseline,
            FilterResult.REJECT),
        args(
            "experience boundary pass",
            job("上海", 25_000, 35_000, 5, 8, "AI Agent Engineer"),
            baseline,
            FilterResult.PASS),
        args(
            "experience unknown",
            job("上海", 25_000, 35_000, null, null, "AI Agent Engineer"),
            baseline,
            FilterResult.UNKNOWN),
        args(
            "experience no minimum unknown",
            job("上海", 25_000, 35_000, null, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.UNKNOWN),
        args(
            "role exact pass",
            job("上海", 25_000, 35_000, 3, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.PASS),
        args(
            "role case pass",
            job("上海", 25_000, 35_000, 3, 5, "ai agent engineer"),
            baseline,
            FilterResult.PASS),
        args(
            "role partial pass",
            job("上海", 25_000, 35_000, 3, 5, "Senior AI Agent"),
            baseline,
            FilterResult.PASS),
        args("role reject", job("上海", 25_000, 35_000, 3, 5, "传统销售"), baseline, FilterResult.REJECT),
        args(
            "unknown plus reject is reject",
            job(null, 10_000, 15_000, 3, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.REJECT),
        args(
            "multiple unknown remains unknown",
            job(null, null, null, null, null, "AI Agent Engineer"),
            baseline,
            FilterResult.UNKNOWN),
        args(
            "all rule boundaries pass",
            job("上海", 20_000, 20_000, 5, 5, "AI Agent Engineer"),
            baseline,
            FilterResult.PASS));
  }

  private static Arguments args(
      String name, Job job, FilterCriteria criteria, FilterResult expected) {
    return Arguments.of(name, job, criteria, expected);
  }

  private static Job job(
      String city,
      Integer salaryMin,
      Integer salaryMax,
      Integer experienceMin,
      Integer experienceMax,
      String title) {
    return new Job(
        UUID.randomUUID(),
        UserId.random(),
        Source.BOSS,
        UUID.randomUUID().toString(),
        title,
        title.toLowerCase(),
        "RoleOS",
        "roleos",
        city,
        salaryMin,
        salaryMax,
        14,
        experienceMin,
        experienceMax,
        "本科",
        "负责 AI Agent 平台",
        "负责 AI Agent 平台",
        NOW,
        RecruiterActivity.TODAY,
        URI.create("https://www.zhipin.com/job_detail/fixture.html"),
        SourceStatus.ACTIVE,
        "d".repeat(64),
        Map.of(),
        NOW,
        NOW,
        NOW,
        NOW);
  }
}
