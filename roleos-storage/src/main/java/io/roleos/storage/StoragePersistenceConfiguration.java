package io.roleos.storage;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** RoleOS 存储模块的 JPA 实体与仓储扫描入口。 */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackages = "io.roleos.storage")
@EnableJpaRepositories(basePackages = "io.roleos.storage")
public class StoragePersistenceConfiguration {}
