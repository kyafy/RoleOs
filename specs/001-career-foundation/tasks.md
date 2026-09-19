# 任务清单：Career Foundation（职业基础）

**输入**：`spec.md`、`plan.md`、`research.md`、`data-model.md`、`contracts/`、`quickstart.md`  
**前置条件**：完成 Phase 1 与 Phase 2 后才可开始任一用户故事。

## Phase 1：准备

- [X] T001 将 `specs/001-career-foundation/contracts/career-foundation.openapi.yaml` 补全为可校验的 OpenAPI 契约，列出统一成功/错误响应和各命令的 `commandId` 字段。
- [X] T002 [P] 为 `roleos-domain/src/main/java/io/roleos/domain/career/`、`roleos-application/src/main/java/io/roleos/application/career/`、`roleos-workflow/src/main/java/io/roleos/workflow/` 建立 Feature 包说明与测试目录。
- [X] T003 [P] 在 `roleos-storage/src/test/java/io/roleos/storage/` 建立 PostgreSQL Testcontainers/Flyway 测试基类，并在 `roleos-boot/src/test/java/io/roleos/boot/` 建立 Golden Scenario 测试骨架。

## Phase 2：基础能力（阻塞所有用户故事）

- [X] T004 在 `roleos-domain/src/main/java/io/roleos/domain/career/` 定义 `UserId`、审计字段、`ProvenanceType`、`ConfirmationStatus` 与 `CareerFact` 不变量：Agent 推断不得成为已确认事实。
- [X] T005 [P] 在 `roleos-domain/src/main/java/io/roleos/domain/career/` 定义 `CommandId`、`IdempotencyRecord` Port 与“同一用户同一 commandId 只产生一次结果”的契约。
- [X] T006 [P] 在 `roleos-domain/src/main/java/io/roleos/domain/career/` 定义 Repository Port；在 `roleos-storage/src/main/java/io/roleos/storage/career/` 创建对应 JPA Adapter 轮廓，禁止 Domain 依赖 JPA。
- [X] T007 在 `roleos-storage/src/main/resources/db/migration/V2__career_foundation_base.sql` 创建用户归属、审计、乐观版本与幂等记录的基础表及唯一约束。
- [X] T008 在 `roleos-web/src/main/java/io/roleos/web/security/` 建立当前认证主体到 `UserId` 的解析器；禁止从任何请求体字段读取或覆盖 `userId`。
- [X] T009 在 `roleos-web/src/test/java/io/roleos/web/security/CurrentUserResolverTest.java` 测试未认证拒绝、跨用户归属拒绝及 Trace ID 响应。

**检查点**：所有 Feature 写操作都有用户归属、来源和幂等基础；安全默认拒绝策略未被放宽。

## Phase 3：用户故事 1 — 建立真实职业档案（P1）

**目标**：用户可独立创建并维护职业档案、经历、项目和自我声明技能。

**独立测试**：创建一份档案、一段经历、一个个人项目、一个技能后重启，全部数据仍存在且只属于当前用户。

- [X] T010 [P] [US1] 在 `roleos-domain/src/main/java/io/roleos/domain/career/profile/` 定义 CareerProfile、Experience、Project 聚合与类型枚举；Experience 可不完整但不可虚构，项目必须保存历史来源/项目性质。
- [X] T011 [P] [US1] 在 `roleos-domain/src/main/java/io/roleos/domain/career/skill/` 定义 Skill；约束 `(userId, normalizedName)` 基础唯一，自评层级仅 `AWARENESS`、`USED`、`WORKING`、`ADVANCED`，验证层级可为空。
- [X] T012 [US1] 在 `roleos-storage/src/main/resources/db/migration/V3__career_profile_assets.sql` 创建档案、经历、项目、技能与来源记录表、外键及上述唯一约束。
- [X] T013 [P] [US1] 在 `roleos-domain/src/test/java/io/roleos/domain/career/` 编写档案归属、项目性质分离、技能名称规范化及层级不变量单元测试，先使测试失败。
- [X] T014 [US1] 在 `roleos-application/src/main/java/io/roleos/application/career/profile/` 实现档案和职业资产 CRUD 用例，写入 `USER_INPUT`/已确认来源并使用 Idempotency Port。
- [X] T015 [US1] 在 `roleos-web/src/main/java/io/roleos/web/api/career/` 实现 `GET/PUT /api/v1/career-profile` 及经历、项目、技能资源 Controller/DTO，并接入 Bean Validation 和统一响应。
- [X] T016 [US1] 在 `roleos-web/src/test/java/io/roleos/web/api/career/CareerProfileControllerTest.java` 覆盖成功、校验失败、跨用户拒绝和敏感字段不出现在普通响应的契约测试。
- [X] T017 [US1] 在 `roleos-boot/src/test/java/io/roleos/boot/CareerProfilePersistenceIT.java` 用 Testcontainers 验证 Flyway、重启后读取和用户归属隔离。

## Phase 4：用户故事 2 — 导入并确认简历信息（P1）

**目标**：用户通过 Fake 导入创建候选项，并逐条确认、编辑或拒绝。

**独立测试**：同一导入批次中确认、编辑、拒绝三条候选项，只有前两条生成或更新职业事实。

- [X] T018 [P] [US2] 在 `roleos-domain/src/main/java/io/roleos/domain/career/importing/` 定义 ResumeImport、FactCandidate、CandidateDecision 与 `WAITING_CONFIRMATION`/`CONFIRM`/`EDIT`/`REJECT` 状态不变量。
- [X] T019 [P] [US2] 在 `roleos-experience/src/main/java/io/roleos/experience/importer/` 定义 `ResumeImportPort` 及结构化提取契约；明确输出不得标记为用户确认事实。
- [X] T020 [US2] 在 `roleos-storage/src/main/resources/db/migration/V4__resume_import_candidates.sql` 创建导入、候选项和候选决定表；候选项保存受控来源定位，原始简历不进入普通日志。
- [X] T021 [P] [US2] 在 `roleos-experience/src/test/java/io/roleos/experience/importer/FakeResumeImportAdapterContractTest.java` 先定义 Fake Adapter 的正常、部分失败、缺失字段与不虚构字段契约。
- [X] T022 [US2] 在 `roleos-experience/src/main/java/io/roleos/experience/importer/FakeResumeImportAdapter.java` 实现确定性 Fake Adapter，仅接收受控 fixture/结构化输入并返回候选项和失败说明。
- [X] T023 [US2] 在 `roleos-application/src/main/java/io/roleos/application/career/importing/` 实现创建批次与逐项决定用例：确认/编辑追加用户确认来源，拒绝只留审计；重复 commandId 返回原结果。
- [X] T024 [US2] 在 `roleos-web/src/main/java/io/roleos/web/api/importing/` 实现 `POST /api/v1/resume-imports` 和 `POST /api/v1/import-candidates/{candidateId}/decisions`。
- [X] T025 [US2] 在 `roleos-web/src/test/java/io/roleos/web/api/importing/ResumeImportControllerTest.java` 验证候选初始待确认、确认/编辑/拒绝、部分成功、409 状态冲突与未确认写入率为零。

## Phase 5：用户故事 3 — 区分技能来源与能力层级（P2）

**目标**：用户可查看技能多来源、两种层级和能力关联，不得到自动评分。

**独立测试**：同一技能加入自我声明与经确认经历来源后均可见，验证层级仍为空。

- [X] T026 [P] [US3] 在 `roleos-domain/src/main/java/io/roleos/domain/career/skill/` 定义 SkillSource、Capability 及 Skill/Experience/Project 关联；禁止在 001 写入自动评分或自动推导结果。
- [X] T027 [US3] 在 `roleos-storage/src/main/resources/db/migration/V5__skill_sources_capabilities.sql` 创建多来源和能力关联表，保持同一 Skill 多条来源。
- [X] T028 [P] [US3] 在 `roleos-domain/src/test/java/io/roleos/domain/career/skill/SkillProvenanceTest.java` 先测试“SELF_DECLARED ≠ VERIFIED”与“自评层级 ≠ 验证层级”。
- [X] T029 [US3] 在 `roleos-application/src/main/java/io/roleos/application/career/skill/` 实现来源、支撑资产和能力关系查询/维护用例。
- [X] T030 [US3] 在 `roleos-web/src/main/java/io/roleos/web/api/skill/` 实现技能/能力查询和维护接口，响应必须分别呈现自评层级、验证层级、来源与支撑资产。
- [X] T031 [US3] 在 `roleos-web/src/test/java/io/roleos/web/api/skill/SkillControllerTest.java` 验证大小写/空格规范化、来源并存、验证层级不被自评覆盖。

## Phase 6：用户故事 4 — 可恢复工作流与人工审批（P2）

**目标**：长期任务可等待审批、跨重启恢复，并抵抗重复或不兼容决定。

**独立测试**：创建任务并进入 `WAITING_APPROVAL`，重启后暂缓、批准或重复提交决定，状态始终合法且只推进一次。

- [X] T032 [P] [US4] 在 `roleos-domain/src/main/java/io/roleos/domain/workflow/` 定义 WorkflowInstance、WorkflowEvent、Approval、状态/类型/决定枚举和合法转换表；终态不可推进。
- [X] T033 [US4] 在 `roleos-storage/src/main/resources/db/migration/V6__durable_workflow_approval.sql` 创建任务、事件和审批表；以版本字段支持条件更新与追加审计。
- [X] T034 [P] [US4] 在 `roleos-workflow/src/test/java/io/roleos/workflow/WorkflowTransitionServiceTest.java` 先测试 Agent 不可直接转移、审批暂缓保持等待、重复 commandId 不重复推进及状态冲突。
- [X] T035 [US4] 在 `roleos-workflow/src/main/java/io/roleos/workflow/WorkflowTransitionService.java` 实现唯一状态转移入口，原子更新实例、事件、审批和幂等记录；不确定外部副作用必须转 `PAUSED_FOR_HUMAN`。
- [X] T036 [US4] 在 `roleos-application/src/main/java/io/roleos/application/career/workflow/` 实现创建/查看任务、处理审批和显式恢复/取消用例；重启不自动重放 RUNNING 动作。
- [X] T037 [US4] 在 `roleos-web/src/main/java/io/roleos/web/api/workflow/` 实现 `/api/v1/workflows`、`/api/v1/approvals` 与审批决定接口，所有写操作要求 commandId。
- [X] T038 [US4] 在 `roleos-boot/src/test/java/io/roleos/boot/WorkflowRecoveryIT.java` 用 Testcontainers 验证等待审批后的应用上下文重启、用户归属、状态/原因/审批恢复和并发决定。

### Phase 6 验证证据（2026-09-18）

- 已完成：T032–T038；所有状态写入均携带 `commandId`，并由数据库事件、审批与幂等记录在同一事务中持久化。
- 通过：`mvn -B -ntp -pl roleos-web -am test -DskipTests=false`（工作流转换 5 项、工作流 Web 契约 2 项及依赖模块测试）。
- 通过：`mvn -B -ntp -pl roleos-boot -am test -Dtest=WorkflowRecoveryIT -Dsurefire.failIfNoSpecifiedTests=false`（PostgreSQL Testcontainers、Flyway V6、三次上下文启动、归属隔离、重复命令与并发决定）。
- 通过：Spotless 格式化与 `git diff --check`；未完成任务：Phase 7 的 T039–T042。

## Phase 7：收尾与跨切面验证

- [X] T039 [P] 在 `roleos-web/src/test/java/io/roleos/web/` 审核所有 Feature 接口的统一错误、Trace ID、认证与脱敏响应契约。
- [X] T040 [P] 在 `roleos-boot/src/test/java/io/roleos/boot/CareerFoundationGoldenScenarioIT.java` 实现 quickstart 的端到端链路：档案 → 导入确认/编辑/拒绝 → 技能多来源 → 等待审批 → 重启恢复 → 审批决定。
- [X] T041 在 `specs/001-career-foundation/quickstart.md` 按实际实现更新可执行命令和验收记录，并运行 `mvn -Pquality verify`。
- [X] T042 在 `README.md` 与 `docs/RoleOS_AI编程指南.md` 更新 Career Foundation 已交付范围、Fake 导入边界与运行入口。

### Phase 7 验证证据（2026-09-19）

- 已完成：T039–T042；新增 Web 跨切面契约测试和 PostgreSQL Testcontainers Golden Scenario，并将 README、开发指南与 quickstart 同步至实际实现边界。
- 通过：`mvn -B -ntp -pl roleos-web -am test -Dtest=CrossCuttingApiContractTest -Dsurefire.failIfNoSpecifiedTests=false`（6 项认证、校验、Trace ID 与脱敏响应断言）。
- 通过：`mvn -B -ntp -pl roleos-boot -am test -Dtest=CareerFoundationGoldenScenarioIT -Dsurefire.failIfNoSpecifiedTests=false`（受控导入候选确认/编辑/拒绝、技能多来源、重启恢复与幂等审批）。
- 通过：`mvn -B -ntp -Pquality -Ddependency-check.skip=true verify`（16 模块、Spotless、Checkstyle、PMD、SpotBugs 与测试）。依赖安全检查按人工指示跳过；未完成任务：无。

## 依赖与执行顺序

`Setup → Foundational → US1 → US2 → US3 → US4 → Polish`。

- US1 是 MVP；US2 依赖 US1 的职业资产写入能力。
- US3 依赖 US1 的 Skill 与资产模型，且可在 US2 完成后并行准备。
- US4 只依赖 Foundational，可与 US2/US3 并行；最终 Golden Scenario 依赖四个故事。

## 并行机会

- Phase 2 中 T005、T006、T008 可在 T004 的领域约定稳定后并行。
- US1：T010、T011、T013 可并行；US2：T018、T019、T021 可并行；US3：T026、T028 可并行；US4：T032、T034 可并行。
- T039 与 T040 可在各故事实现完成后并行。

## 实施策略

先完成 T001–T017 并独立演示 US1；随后交付导入审阅（US2）、技能可信度视图（US3）和持久化审批（US4）。每个检查点先运行对应测试，再进入下一阶段；不得通过删除审批、将推断写为事实或放宽安全策略来通过测试。
