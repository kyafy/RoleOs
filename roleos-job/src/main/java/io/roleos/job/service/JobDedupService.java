package io.roleos.job.service;

import io.roleos.job.domain.Job;
import io.roleos.job.port.JobRepositories.JobRepository;
import java.time.Instant;
import java.util.Objects;

/** 以来源身份和内容摘要进行双层去重，并保持 Canonical Job 标识稳定。 */
public final class JobDedupService {

  private final JobRepository jobs;

  public JobDedupService(JobRepository jobs) {
    this.jobs = Objects.requireNonNull(jobs);
  }

  /**
   * 解析待写入岗位。来源身份相同则更新事实；仅内容相同则视为重复快照并保留原始外部身份。
   *
   * @param incoming 本次采集并完成标准化的岗位
   * @return 已持久化的 Canonical Job
   */
  public Job resolve(Job incoming) {
    Objects.requireNonNull(incoming, "待去重岗位不能为空");
    return jobs.findBySourceIdentity(
            incoming.userId(), incoming.source().name(), incoming.externalJobId())
        .map(existing -> jobs.save(mergeSourceUpdate(existing, incoming)))
        .orElseGet(
            () ->
                jobs.findByContentIdentity(
                        incoming.userId(),
                        incoming.normalizedCompany(),
                        incoming.normalizedTitle(),
                        incoming.city(),
                        incoming.contentHash())
                    .map(existing -> jobs.save(refreshDuplicate(existing, incoming)))
                    .orElseGet(() -> jobs.save(incoming)));
  }

  private static Job mergeSourceUpdate(Job existing, Job incoming) {
    return new Job(
        existing.id(),
        existing.userId(),
        existing.source(),
        existing.externalJobId(),
        incoming.title(),
        incoming.normalizedTitle(),
        incoming.company(),
        incoming.normalizedCompany(),
        incoming.city(),
        incoming.salaryMin(),
        incoming.salaryMax(),
        incoming.salaryMonths(),
        incoming.experienceMin(),
        incoming.experienceMax(),
        incoming.educationRequirement(),
        incoming.rawJd(),
        incoming.normalizedJd(),
        incoming.publishTime(),
        incoming.recruiterActivity(),
        incoming.sourceUrl(),
        incoming.sourceStatus(),
        incoming.contentHash(),
        incoming.metadata(),
        existing.firstSeenAt(),
        latest(existing.lastSeenAt(), incoming.lastSeenAt()),
        existing.createdAt(),
        incoming.updatedAt());
  }

  private static Job refreshDuplicate(Job existing, Job incoming) {
    return new Job(
        existing.id(),
        existing.userId(),
        existing.source(),
        existing.externalJobId(),
        existing.title(),
        existing.normalizedTitle(),
        existing.company(),
        existing.normalizedCompany(),
        existing.city(),
        existing.salaryMin(),
        existing.salaryMax(),
        existing.salaryMonths(),
        existing.experienceMin(),
        existing.experienceMax(),
        existing.educationRequirement(),
        existing.rawJd(),
        existing.normalizedJd(),
        existing.publishTime(),
        existing.recruiterActivity(),
        existing.sourceUrl(),
        existing.sourceStatus(),
        existing.contentHash(),
        existing.metadata(),
        existing.firstSeenAt(),
        latest(existing.lastSeenAt(), incoming.lastSeenAt()),
        existing.createdAt(),
        incoming.updatedAt());
  }

  private static Instant latest(Instant first, Instant second) {
    return first.isAfter(second) ? first : second;
  }
}
