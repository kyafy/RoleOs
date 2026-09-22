package io.roleos.job.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.roleos.domain.career.UserId;
import io.roleos.job.domain.Job;
import io.roleos.job.domain.JobTypes.RecruiterActivity;
import io.roleos.job.domain.JobTypes.Source;
import io.roleos.job.domain.JobTypes.SourceStatus;
import io.roleos.job.port.JobRepositories.JobRepository;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 双层去重必须稳定 Canonical Job ID，避免 Candidate/Decision 被新快照替换。 */
class JobDedupServiceTest {

  private static final Instant FIRST_SEEN = Instant.parse("2026-09-19T01:00:00Z");
  private static final Instant LATER = Instant.parse("2026-09-19T02:00:00Z");

  @Test
  void sourceIdentityUpdateKeepsCanonicalIdAndFirstSeenTime() {
    MemoryJobs jobs = new MemoryJobs();
    UserId userId = UserId.random();
    Job original = job(userId, "boss-1", "初始 JD", "a".repeat(64), FIRST_SEEN);
    jobs.save(original);

    Job resolved =
        new JobDedupService(jobs).resolve(job(userId, "boss-1", "更新 JD", "b".repeat(64), LATER));

    assertThat(resolved.id()).isEqualTo(original.id());
    assertThat(resolved.firstSeenAt()).isEqualTo(FIRST_SEEN);
    assertThat(resolved.rawJd()).isEqualTo("更新 JD");
    assertThat(jobs.values).hasSize(1);
  }

  @Test
  void contentIdentityDuplicateKeepsOriginalExternalIdentity() {
    MemoryJobs jobs = new MemoryJobs();
    UserId userId = UserId.random();
    Job original = job(userId, "boss-1", "相同 JD", "c".repeat(64), FIRST_SEEN);
    jobs.save(original);

    Job resolved =
        new JobDedupService(jobs).resolve(job(userId, "boss-copy", "相同 JD", "c".repeat(64), LATER));

    assertThat(resolved.id()).isEqualTo(original.id());
    assertThat(resolved.externalJobId()).isEqualTo("boss-1");
    assertThat(resolved.lastSeenAt()).isEqualTo(LATER);
    assertThat(jobs.values).hasSize(1);
  }

  @Test
  void sameJdHashFromDifferentCompanyIsNotMerged() {
    MemoryJobs jobs = new MemoryJobs();
    UserId userId = UserId.random();
    Job original = job(userId, "boss-1", "相同 JD", "c".repeat(64), FIRST_SEEN);
    jobs.save(original);
    Job otherCompany =
        copyWithCompany(job(userId, "boss-2", "相同 JD", "c".repeat(64), LATER), "Other");

    Job resolved = new JobDedupService(jobs).resolve(otherCompany);

    assertThat(resolved.id()).isNotEqualTo(original.id());
    assertThat(jobs.values).hasSize(2);
  }

  private Job copyWithCompany(Job job, String company) {
    return new Job(
        job.id(),
        job.userId(),
        job.source(),
        job.externalJobId(),
        job.title(),
        job.normalizedTitle(),
        company,
        company.toLowerCase(),
        job.city(),
        job.salaryMin(),
        job.salaryMax(),
        job.salaryMonths(),
        job.experienceMin(),
        job.experienceMax(),
        job.educationRequirement(),
        job.rawJd(),
        job.normalizedJd(),
        job.publishTime(),
        job.recruiterActivity(),
        job.sourceUrl(),
        job.sourceStatus(),
        job.contentHash(),
        job.metadata(),
        job.firstSeenAt(),
        job.lastSeenAt(),
        job.createdAt(),
        job.updatedAt());
  }

  private Job job(UserId userId, String externalId, String jd, String hash, Instant seenAt) {
    return new Job(
        UUID.randomUUID(),
        userId,
        Source.BOSS,
        externalId,
        "AI Agent 工程师",
        "ai agent 工程师",
        "RoleOS",
        "roleos",
        "上海",
        20_000,
        30_000,
        14,
        3,
        5,
        "本科",
        jd,
        jd,
        seenAt,
        RecruiterActivity.TODAY,
        URI.create("https://www.zhipin.com/job_detail/" + externalId + ".html"),
        SourceStatus.ACTIVE,
        hash,
        Map.of(),
        seenAt,
        seenAt,
        seenAt,
        seenAt);
  }

  private static final class MemoryJobs implements JobRepository {
    private final Map<UUID, Job> values = new LinkedHashMap<>();

    @Override
    public Optional<Job> findBySourceIdentity(UserId userId, String source, String externalJobId) {
      return values.values().stream()
          .filter(job -> job.userId().equals(userId))
          .filter(job -> job.source().name().equals(source))
          .filter(job -> job.externalJobId().equals(externalJobId))
          .findFirst();
    }

    @Override
    public Optional<Job> findByContentIdentity(
        UserId userId,
        String normalizedCompany,
        String normalizedTitle,
        String city,
        String contentHash) {
      return values.values().stream()
          .filter(job -> job.userId().equals(userId))
          .filter(job -> job.normalizedCompany().equals(normalizedCompany))
          .filter(job -> job.normalizedTitle().equals(normalizedTitle))
          .filter(job -> java.util.Objects.equals(job.city(), city))
          .filter(job -> job.contentHash().equals(contentHash))
          .findFirst();
    }

    @Override
    public Optional<Job> findById(UserId userId, UUID jobId) {
      return Optional.ofNullable(values.get(jobId));
    }

    @Override
    public Job save(Job job) {
      values.put(job.id(), job);
      return job;
    }
  }
}
