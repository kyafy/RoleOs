<!--
Sync Impact Report
- 版本变更：模板（未采纳）→ 1.0.0
- 修改原则：无；首次定义六项核心原则。
- 新增章节：技术与安全约束、开发流程与质量门禁、治理。
- 移除章节：无。
- 后续待办：无。
-->

# RoleOS Constitution

## Core Principles

### I. 真实性与证据优先（NON-NEGOTIABLE）

RoleOS MUST 区分 `User Fact`、`Agent Inference`、`Designed Scenario` 与
`Upgraded Capability`。系统不得虚构经历、客户、用户数、收入、生产规模、性能指标
或历史项目。关键 Career Fact 与 Claim MUST 记录 Provenance；简历仅可使用
`VERIFIED` 或 `SUPPORTED` Claim。出现真实 Skill Gap 时，MUST 先完成真实项目升级、
测试/评测与 Evidence 沉淀，再生成对应 Claim。此原则确保职业叙事可验证、可解释，
并能经受面试追问。

### II. 人工拥有最终控制权（NON-NEGOTIABLE）

Experience Confirmation、重大 Project Upgrade、最终 Resume、正式 Application、
验证码/人工验证及任何未知外部副作用 MUST 经过 Human Approval。Agent 或自动化适配器
不得绕过、删除或默认批准这些关卡。对于 `SUBMIT_APPLICATION`、发送消息和撤回投递等
副作用操作，MUST 同时具备幂等性；若无法确定操作是否已生效，MUST 进入
`PAUSED_FOR_HUMAN`，不得盲目重试。

### III. Workflow 在外，Agent 在内

Career Workflow MUST 由 RoleOS 以 PostgreSQL 持久化状态机维护，并支持服务重启后的
暂停与恢复。Java/Workflow 代码 MUST 负责状态转换、幂等、重试和确定性规则；Agent
仅能返回经 Schema 校验的结构化 Decision、Recommendation 或 Plan。Agent MUST NOT
直接写入 `currentStage` 或其他 Workflow State；唯一合法路径为
`Agent Decision → Workflow Rule → Persisted Transition`。可由稳定规则完成的任务
MUST 使用确定性代码，而不是 LLM 推理。

### IV. 领域独立与可替换集成

RoleOS MUST 保持 Career Domain 独立于 OryxOS、Playwright、Kimi 和 Codex 的具体实现：
Domain 仅依赖 Port，外部能力通过 Adapter 接入。仅 `roleos-runtime-oryx` 可以直接依赖
OryxOS Runtime API。浏览器分层固定为
`Career Domain → JobSiteAdapter → BrowserRouter → BrowserProvider`，Domain 中不得出现
页面句柄、CSS Selector、XPath 或 Provider 专属状态。新外部能力 MUST 优先按
`Port → Fake Adapter → Contract Test → Real Adapter` 交付，以保证核心流程不被外部系统
阻塞且可被替换。

### V. 独立验证与可观测交付

Codex 返回 `completed` MUST NOT 被视为 Project Upgrade 成功。升级完成前，RoleOS MUST
独立执行 Build、Test、Evaluation 和 Required Artifact Check，并记录 `PASS`、`PARTIAL`
或 `FAIL`。所有 Task MUST 包含相应测试；确定性逻辑 MUST 有单元测试，持久化、Agent、
Adapter、Workflow Recovery 和端到端场景按风险提供相应测试。关键流程 MUST 具备 Trace、
Audit 与可追溯日志，使决策、事实来源和外部动作可以审查。

### VI. Golden Scenario 与最小复杂度

V1 的首要目标是以真实 Job、真实 Career Profile 和真实 Project Repository 跑通
`Job → Match → Mining → Upgrade → Evidence → Resume → Approval → Application → Outcome`
闭环。实现 MUST 优先服务该闭环，采用 Java 21、Spring Boot 3.x、Maven 多模块、
PostgreSQL、Spring Data JPA 和 Flyway 的模块化单体基线。未经明确的架构变更，MUST NOT
引入微服务、Kafka、Kubernetes、Redis Workflow Engine、Temporal、Graph DB、高级向量
基础设施或复杂 Multi-Agent Network。复杂度后置是保障交付可验证性的必要条件。

## 技术与安全约束

- Career Database MUST 保存 Experience、Project、Skill、Capability、Job、Requirement、
  Gap、Claim、Evidence、Workflow、Resume、Application 与 Outcome 等结构化职业事实；
  Agent Memory 仅保存偏好、对话和语义上下文。
- `AGENT_INFERENCE` MUST NOT 在未经用户确认或 Evidence 支持时升级为用户事实。反馈结果
  MUST 先记录为 Observation/Feedback/Recommendation，不能直接降低或修改 Career Fact。
- 项目升级 MUST 在独立 Git Worktree/Branch 中完成，升级分支命名为
  `roleos/upgrade/{upgradeId}`，不得直接修改用户主分支。
- Browser Provider 切换时 MUST NOT 迁移 ElementRef、DOM Snapshot、Tab Handle 或 Browser
  Session；MUST 从 `BrowserTaskState` 重新导航并生成新的快照后恢复业务步骤。
- Broad Job MUST 使用低成本处理路径；Experience Mining、Project Upgrade 与岗位定制简历
  MUST NOT 默认作用于全部 Broad Job。
- 个人职业信息、认证信息与 Secret MUST 最小化收集、受控存储、可审计，且不得输出到日志、
  测试夹具或版本控制系统。

## 开发流程与质量门禁

- 主体功能 MUST 遵循 Spec-Driven Development：`Constitution → Spec → Plan → Tasks →
  Implement → Test/Analyze → Demo`。仅小型、隔离的增量可使用明确的任务契约。
- 每次实现 MUST 从一个小而可独立验收的 Task 开始：先阅读相关事实来源与现有代码，再明确
  最小受影响模块/文件、验收条件和测试；不得进行无关重构或创建平行 Service。
- 每个 User Story 完成时 MUST 验证 Constitution、Spec、Plan、Tasks 与代码的一致性，运行
  相关测试并提供可运行 Demo、必要 Trace 与人工 Review；不得通过修改本宪法来掩盖实现冲突。
- 涉及新增 Maven Module、修改 Domain Boundary、引入基础设施、增加 Workflow Type 或跨越
  三项以上核心业务能力的变更 MUST 重新执行 Spec、Plan 和 Tasks 流程。
- V1 发布前 MUST 使用真实数据完成一次 Golden Scenario E2E，且 Workflow 中断后能够恢复，
  Claim 与 Evidence 符合本宪法的真实性边界。

## Governance

本宪法高于仓库内的一般开发惯例。所有 Spec、Plan、Task、代码评审与发布检查 MUST 验证其
符合性。任何与核心原则冲突的实现 MUST 停止并提交给项目维护者决策，不得擅自放宽规范。

修订 MUST 说明动机、受影响的原则/章节、迁移或回滚方案，并经项目维护者批准。版本采用
语义化规则：删除或不兼容地重定义治理原则时递增 MAJOR；新增原则或实质扩展约束时递增
MINOR；不改变治理含义的澄清与措辞修正递增 PATCH。每次修订 MUST 更新本文件顶部的
Sync Impact Report 和 `Last Amended` 日期；该报告供人工审阅，应在正式提交前移除。

`AGENTS.md` 提供仓库级的日常执行细则；`docs/RoleOS需求文档.md`、
`docs/RoleOS技术方案.md` 和 `docs/RoleOS_AI编程指南.md` 是本宪法的产品、架构与交付
依据，但不得与本宪法冲突。

**Version**: 1.0.0 | **Ratified**: 2026-09-16 | **Last Amended**: 2026-09-16
