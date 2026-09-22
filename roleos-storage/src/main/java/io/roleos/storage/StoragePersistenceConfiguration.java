package io.roleos.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.roleos.storage.career.CareerUserJdbcSupport;
import io.roleos.storage.job.JdbcJobCandidateRepositoryAdapter;
import io.roleos.storage.job.JdbcJobDecisionRepositoryAdapter;
import io.roleos.storage.job.JdbcJobRepositoryAdapter;
import io.roleos.storage.job.JdbcJobSearchRepositoryAdapter;
import io.roleos.storage.job.JdbcRankingEvaluationRepositoryAdapter;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** RoleOS 存储模块的 JPA 实体与仓储扫描入口。 */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackages = "io.roleos.storage")
@EnableJpaRepositories(basePackages = "io.roleos.storage")
@ComponentScan(
    basePackageClasses = {
      CareerUserJdbcSupport.class,
      JdbcJobCandidateRepositoryAdapter.class,
      JdbcJobDecisionRepositoryAdapter.class,
      JdbcJobRepositoryAdapter.class,
      JdbcJobSearchRepositoryAdapter.class,
      JdbcRankingEvaluationRepositoryAdapter.class
    })
public class StoragePersistenceConfiguration {

  /**
   * 为尚使用 Jackson 2 API 的数据库 JSON 适配器提供受控兼容 Bean。
   *
   * <p>HTTP 层由 Spring Boot 4 的 Jackson 3 自动配置负责；该 Bean 仅服务于已持久化 JSON 的 Adapter，避免把 Jackson 2 类型泄漏到
   * Web 契约。
   */
  @Bean
  ObjectMapper persistenceObjectMapper() {
    return new ObjectMapper();
  }
}
