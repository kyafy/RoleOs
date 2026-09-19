# 实施计划：Career Foundation（职业基础）

**分支**：`001-career-foundation` | **日期**：2026-09-17 | **规格**：[spec.md](./spec.md)

## 摘要

交付职业事实、简历候选审阅、技能来源与持久化工作流基础。使用既有 Java 21、Spring Boot、PostgreSQL/Flyway 模块化单体；真实简历解析不在范围内，首期以 Fake Adapter 支持可重复验收。

## 技术上下文

**语言/版本**：Java 21；**主要依赖**：Spring Boot、Spring MVC/Security、JPA、Flyway、OpenAPI；**存储**：PostgreSQL；**测试**：JUnit、MockMvc、Testcontainers、契约和 E2E 测试；**平台**：单机 Web 应用；**范围**：FR-001～FR-018。

**约束**：所有资产带 `userId` 与来源；候选与 Agent 推断未经确认不得成为职业事实；审批/状态转移幂等；敏感信息不可写入日志或夹具；不引入工作流引擎、消息队列或图数据库。

## 宪法检查

| 门禁 | 结论 |
| --- | --- |
| 真实性与证据优先 | 通过：FactCandidate 必须确认/编辑后入库；推断只作非事实记录。 |
| 人工最终控制 | 通过：审批持久化，自动建议不能越过审批。 |
| Workflow 在外，Agent 在内 | 通过：仅 `roleos-workflow` 可转移状态。 |
| 领域独立与可替换集成 | 通过：ResumeImportPort + Fake Adapter；未引入 Provider 依赖。 |
| 验证与可观测 | 通过：单元、持久化、契约、恢复、E2E 分层测试。 |
| 最小复杂度 | 通过：复用既有模块化单体与 PostgreSQL。 |

Phase 1 复核：通过，无复杂度例外。

## 项目结构

```text
roleos-domain/.../career/       # 事实、值对象、Port、不变量
roleos-application/.../career/  # 用例与归属校验
roleos-workflow/...             # 状态机、审批、幂等
roleos-experience/.../importer/ # ResumeImportPort 与 Fake Adapter
roleos-storage/...              # JPA/Repository/Flyway
roleos-web/.../api/             # REST DTO 与 Controller
roleos-boot/...                 # Testcontainers 与 Golden Scenario
```

`roleos-domain` 不依赖 Web、JPA 或解析实现；Web 只调用应用层；Workflow 是唯一状态变更入口。
