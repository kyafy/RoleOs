package io.roleos.job.port;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobCandidate.FilterEvaluation;
import io.roleos.job.domain.JobCandidate.RankingEvaluation;
import io.roleos.job.domain.JobSearch;
import io.roleos.job.domain.JobSearch.JobDecision;
import io.roleos.job.domain.JobSearch.SourceSnapshot;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.JobStrategy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Job Intelligence 持久化端口集合；所有读取都必须带用户归属或已验证父实体。 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class JobRepositories {

  private JobRepositories() {}

  public interface JobRepository {
    Optional<Job> findBySourceIdentity(UserId userId, String source, String externalJobId);

    Optional<Job> findByContentIdentity(
        UserId userId,
        String normalizedCompany,
        String normalizedTitle,
        String city,
        String contentHash);

    Optional<Job> findById(UserId userId, UUID jobId);

    Job save(Job job);
  }

  public interface SourceSnapshotRepository {
    Optional<SourceSnapshot> findByJobAndContentHash(UUID jobId, String contentHash);

    SourceSnapshot save(SourceSnapshot snapshot);
  }

  public interface JobCandidateRepository {
    Optional<JobCandidate> find(UserId userId, UUID jobId);

    List<JobCandidate> findAll(
        UserId userId, JobStrategy strategy, FilterResult filterResult, int offset, int limit);

    long count(UserId userId, JobStrategy strategy, FilterResult filterResult);

    JobCandidate save(JobCandidate candidate);

    FilterEvaluation saveFilterEvaluation(FilterEvaluation evaluation);

    Optional<FilterEvaluation> findFilterEvaluation(UUID candidateId);
  }

  public interface RankingEvaluationRepository {
    RankingEvaluation save(RankingEvaluation evaluation);

    Optional<RankingEvaluation> find(UUID candidateId);
  }

  public interface JobDecisionRepository {
    Optional<JobDecision> findByCommandId(UserId userId, String commandId);

    JobDecision save(JobDecision decision, JobCandidate resultingCandidate);
  }

  public interface JobSearchRepository {
    Optional<JobSearch> find(UserId userId, UUID searchId);

    Optional<JobSearch> findByStartCommandId(UserId userId, String commandId);

    Optional<JobSearch> findByResumeCommandId(UserId userId, String commandId);

    JobSearch save(JobSearch search);
  }
}
