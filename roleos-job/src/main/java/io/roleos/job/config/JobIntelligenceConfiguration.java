package io.roleos.job.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.domain.agent.AgentRuntimePort;
import io.roleos.job.port.JobRepositories.JobCandidateRepository;
import io.roleos.job.port.JobRepositories.JobDecisionRepository;
import io.roleos.job.port.JobRepositories.JobRepository;
import io.roleos.job.port.JobRepositories.JobSearchRepository;
import io.roleos.job.port.JobRepositories.RankingEvaluationRepository;
import io.roleos.job.port.JobRepositories.SourceSnapshotRepository;
import io.roleos.job.port.JobSourcePort;
import io.roleos.job.port.JobTelemetry;
import io.roleos.job.port.SemanticRankingPort;
import io.roleos.job.service.AgentRuntimeSemanticRankingAdapter;
import io.roleos.job.service.BroadRankingService;
import io.roleos.job.service.BroadRankingService.RankingConfig;
import io.roleos.job.service.JobDiscoveryService;
import io.roleos.job.service.JobHardFilterService;
import io.roleos.job.service.JobNormalizationService;
import io.roleos.job.service.JobPoolService;
import io.roleos.job.service.JobSearchOrchestrator;
import io.roleos.job.service.JobStrategyService;
import io.roleos.job.service.SemanticRankingValidator;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Job Intelligence 的确定性服务装配；外部 Source/Repository 由 Adapter 模块提供。 */
@Configuration(proxyBeanMethods = false)
public class JobIntelligenceConfiguration {

  @Bean
  public JobNormalizationService jobNormalizationService(Clock clock) {
    return new JobNormalizationService(clock);
  }

  @Bean
  public JobHardFilterService jobHardFilterService(Clock clock) {
    return new JobHardFilterService(clock, "hard-filter-v1");
  }

  @Bean
  public SemanticRankingValidator semanticRankingValidator(ObjectMapper objectMapper) {
    return new SemanticRankingValidator(objectMapper);
  }

  @Bean
  public SemanticRankingPort semanticRankingPort(
      AgentRuntimePort runtime, SemanticRankingValidator validator) {
    return new AgentRuntimeSemanticRankingAdapter(runtime, validator);
  }

  @Bean
  public BroadRankingService broadRankingService(
      Clock clock,
      SemanticRankingPort semanticRankingPort,
      @Value("${roleos.job.ranking.rule-weight:0.45}") double ruleWeight,
      @Value("${roleos.job.ranking.semantic-weight:0.55}") double semanticWeight,
      @Value("${roleos.job.ranking.recruiter-activity-max-points:5}") int activityMaxPoints,
      @Value("${roleos.job.ranking.targeted-threshold:80}") int targetedThreshold) {
    return new BroadRankingService(
        clock,
        semanticRankingPort,
        new RankingConfig(
            ruleWeight, semanticWeight, activityMaxPoints, targetedThreshold, "ranking-v1"));
  }

  @Bean
  public JobStrategyService jobStrategyService(
      JobCandidateRepository candidates, JobDecisionRepository decisions, Clock jobClock) {
    return new JobStrategyService(candidates, decisions, jobClock);
  }

  @Bean
  public JobPoolService jobPoolService(
      JobRepository jobs, JobCandidateRepository candidates, RankingEvaluationRepository rankings) {
    return new JobPoolService(jobs, candidates, rankings);
  }

  @Bean
  public JobDiscoveryService jobDiscoveryService(
      JobSourcePort source,
      JobNormalizationService normalization,
      JobRepository jobs,
      SourceSnapshotRepository snapshots,
      JobCandidateRepository candidates,
      JobHardFilterService hardFilter,
      Clock clock) {
    return new JobDiscoveryService(
        source, normalization, jobs, snapshots, candidates, hardFilter, clock);
  }

  @Bean
  public JobSearchOrchestrator jobSearchOrchestrator(
      JobSearchRepository searches,
      JobCandidateRepository candidates,
      RankingEvaluationRepository rankings,
      JobDiscoveryService discovery,
      BroadRankingService ranking,
      Clock clock,
      JobTelemetry telemetry) {
    return new JobSearchOrchestrator(
        searches, candidates, rankings, discovery, ranking, clock, telemetry);
  }

  @Bean
  public JobTelemetry jobTelemetry() {
    return JobTelemetry.NOOP;
  }
}
