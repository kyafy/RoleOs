package io.roleos.job.service;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobCandidate;
import io.roleos.job.domain.JobCandidate.FilterEvaluation;
import io.roleos.job.domain.JobCandidate.RankingEvaluation;
import io.roleos.job.domain.JobTypes.FilterResult;
import io.roleos.job.domain.JobTypes.JobStrategy;
import io.roleos.job.port.JobRepositories.JobCandidateRepository;
import io.roleos.job.port.JobRepositories.JobRepository;
import io.roleos.job.port.JobRepositories.RankingEvaluationRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** 查询当前用户 Job Pool 的轻量应用服务。 */
public final class JobPoolService {

  private final JobRepository jobs;
  private final JobCandidateRepository candidates;
  private final RankingEvaluationRepository rankings;

  public JobPoolService(
      JobRepository jobs, JobCandidateRepository candidates, RankingEvaluationRepository rankings) {
    this.jobs = Objects.requireNonNull(jobs);
    this.candidates = Objects.requireNonNull(candidates);
    this.rankings = Objects.requireNonNull(rankings);
  }

  public List<JobPoolEntry> list(
      UserId userId, JobStrategy strategy, FilterResult filter, int offset, int limit) {
    return candidates.findAll(userId, strategy, filter, offset, limit).stream()
        .map(candidate -> entry(userId, candidate))
        .flatMap(Optional::stream)
        .toList();
  }

  public Optional<JobPoolEntry> find(UserId userId, UUID jobId) {
    return candidates.find(userId, jobId).flatMap(candidate -> entry(userId, candidate));
  }

  public long count(UserId userId, JobStrategy strategy, FilterResult filter) {
    return candidates.count(userId, strategy, filter);
  }

  private Optional<JobPoolEntry> entry(UserId userId, JobCandidate candidate) {
    return jobs.findById(userId, candidate.jobId())
        .map(
            job ->
                new JobPoolEntry(
                    job,
                    candidate,
                    candidates.findFilterEvaluation(candidate.id()).orElse(null),
                    rankings.find(candidate.id()).orElse(null)));
  }

  public record JobPoolEntry(
      Job job,
      JobCandidate candidate,
      FilterEvaluation filterEvaluation,
      RankingEvaluation rankingEvaluation) {}
}
