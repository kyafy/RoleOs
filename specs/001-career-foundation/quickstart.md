# Career Foundation 验证指南

## 本地启动

前置条件：JDK 21、Maven 3.9+、Docker Compose。

```bash
bash scripts/dev-up.sh
```

服务启动后，可访问 `http://localhost:8080/actuator/health` 确认健康状态。受保护的 Career Foundation
接口使用当前登录用户作为用户归属，不能以请求体中的用户标识替代认证。

## 可复现验收命令

以下命令在仓库根目录执行；不要并行运行多个 Maven Reactor 构建，以免共享的 `target/` 目录相互覆盖。

```bash
# Feature Web 契约（认证、错误响应、Trace ID、脱敏）
mvn -B -ntp -pl roleos-web -am test \
  -Dtest=CrossCuttingApiContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false

# PostgreSQL + Flyway Golden Scenario（需要可用的 Docker daemon）
mvn -B -ntp -pl roleos-boot -am test \
  -Dtest=CareerFoundationGoldenScenarioIT \
  -Dsurefire.failIfNoSpecifiedTests=false

# 完整质量门禁：格式、静态分析、依赖安全检查与全部单元测试
mvn -B -ntp -Pquality verify
```

## 验收链路与边界

`CareerFoundationGoldenScenarioIT` 使用受控的 `fixture://` 导入载荷，验证如下完整链路：

1. 创建职业档案。
2. 导入候选经历、技能和项目，并分别确认、编辑和拒绝；只有确认或编辑后的候选项会写入职业资产。
3. 为同一技能记录自我声明和确认经历两个来源；自评等级不会被当作已验证等级。
4. 创建等待审批的长期工作流，重启应用后恢复其状态与审批。
5. 提交审批决定，再重复同一 `commandId`，验证不会重复推进。

Fake 导入只用于受控开发和测试，**不是**简历解析器，也不应把夹具内容、原始简历或未经确认的候选项视作用户事实。参照
[数据模型](./data-model.md) 与 [OpenAPI 契约](./contracts/career-foundation.openapi.yaml) 检查来源、归属、错误响应和敏感信息边界。

## 验收记录（2026-09-19）

- 通过：`CrossCuttingApiContractTest`，6 项断言覆盖认证、校验错误、Trace ID 与未处理异常的通用脱敏响应。
- 通过：`CareerFoundationGoldenScenarioIT`，使用 PostgreSQL Testcontainers 与 Flyway 验证导入确认、技能多来源、重启恢复和幂等审批决定。
- 通过：`mvn -B -ntp -Pquality -Ddependency-check.skip=true verify`，16 个模块均成功通过格式、Checkstyle、PMD、SpotBugs 与测试。
- 例外：依赖安全检查按人工指示跳过；恢复时执行 `mvn -B -ntp -Pquality verify`，建议同时配置 `NVD_API_KEY` 以避免首次漏洞库同步耗时过长。
