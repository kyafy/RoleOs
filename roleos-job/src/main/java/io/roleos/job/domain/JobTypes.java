package io.roleos.job.domain;

/** Job Intelligence 的稳定枚举集合，避免 Adapter 自行发明状态和值。 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class JobTypes {

  private JobTypes() {}

  public enum Source {
    BOSS
  }

  public enum SourceStatus {
    ACTIVE,
    REMOVED,
    UNKNOWN
  }

  public enum FilterResult {
    PASS,
    REJECT,
    UNKNOWN
  }

  public enum JobStrategy {
    BROAD_APPLY,
    TARGETED_APPLY,
    SKIPPED
  }

  public enum StrategyRecommendation {
    KEEP_BROAD,
    PROMOTE_TO_TARGETED,
    SKIP,
    REVIEW
  }

  public enum SemanticAnalysisStatus {
    PENDING,
    SUCCEEDED,
    FAILED
  }

  public enum RecruiterActivity {
    JUST_NOW,
    TODAY,
    RECENT,
    STALE,
    UNKNOWN
  }

  public enum SearchState {
    CREATED,
    RUNNING,
    PAUSED_FOR_HUMAN,
    RETRYABLE_FAILED,
    FAILED,
    COMPLETED
  }

  public enum SearchStep {
    NAVIGATE_SEARCH,
    LIST_SNAPSHOT,
    DETAIL_FETCH,
    NORMALIZE,
    FILTER,
    RANK,
    PERSIST
  }

  public enum BrowserProviderType {
    AUTO,
    PLAYWRIGHT_MCP,
    KIMI_WEBBRIDGE,
    FAKE
  }

  public enum ParseStatus {
    PENDING,
    SUCCEEDED,
    FAILED
  }

  public enum DecisionType {
    PROMOTE,
    KEEP_BROAD,
    SKIP
  }
}
