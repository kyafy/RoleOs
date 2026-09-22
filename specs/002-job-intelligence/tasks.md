# Tasks：Job Intelligence（岗位智能）

**输入**：`specs/002-job-intelligence/` 下的 spec、plan、research、data-model、contracts 与 quickstart
**组织原则**：按用户故事分 Phase；每项任务只有在实现、对应测试与适用质量门禁通过后才能标记 `[X]`。

## 格式：`[ID] [P?] [Story] 描述`

- **[P]**：不同文件且无未完成前置依赖，可并行
- **[USn]**：映射到 `spec.md` 用户故事

## Phase 1：Setup（共享依赖与模块边界）

**目标**：准备 Feature 002 所需依赖、配置和包结构，不实现业务。

- [X] T001 在 `pom.xml` 引入 MCP Java SDK 2.0.0 BOM，并在 `roleos-browser/pom.xml` 增加 `mcp-core`、`mcp-json-jackson2` 依赖
- [X] T002 在 `roleos-web/pom.xml` 增加 `spring-boot-starter-thymeleaf`，并在 `roleos-storage/pom.xml`、`roleos-web/pom.xml`、`roleos-browser/pom.xml` 建立对 `roleos-job` 的单向依赖
- [X] T003 [P] 在 `roleos-boot/src/main/resources/application.yaml` 增加 Job 搜索、Ranking 权重、Playwright MCP 与 Kimi Bridge 的分环境配置及非敏感启动日志开关
- [X] T004 [P] 在 `roleos-job/src/main/java/io/roleos/job/package-info.java` 和 `roleos-browser/src/main/java/io/roleos/browser/package-info.java` 写明模块边界、注释规范和禁止依赖
- [X] T005 在 `roleos-boot/src/test/java/io/roleos/boot/ModuleBoundaryTest.java` 增加断言：`roleos-job/domain` 不引用 browser/MCP/Web/Storage，Career Domain 不引用 Playwright/Kimi/CSS/XPath

**Checkpoint**：`mvn -B -ntp -pl roleos-job,roleos-browser,roleos-storage,roleos-web -am test -DskipTests` 可编译。

**Phase 1 证据（2026-09-19）**：`mvn -B -ntp -pl roleos-boot -am test -Dtest=ModuleBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false` 通过；16 个 Reactor 模块编译成功，边界测试 2/2 通过。

---

## Phase 2：Foundational（阻塞所有用户故事）

**目标**：建立共享值对象、端口、数据库与错误/日志契约。

- [X] T006 [P] 在 `roleos-job/src/main/java/io/roleos/job/domain/JobTypes.java` 定义 Source、FilterResult、JobStrategy、StrategyRecommendation、SemanticAnalysisStatus、RecruiterActivity、SearchState、SearchStep、BrowserProviderType 枚举
- [X] T007 [P] 在 `roleos-job/src/main/java/io/roleos/job/domain/Job.java` 定义 Canonical Job，落实 nullable Unknown、HTTPS Boss URL、0..60 年经验、非负薪资及 SHA-256 contentHash 校验
- [X] T008 [P] 在 `roleos-job/src/main/java/io/roleos/job/domain/JobCandidate.java` 定义 Candidate、FilterEvaluation、RankingEvaluation 与版本字段，确保推荐不能修改 strategy、分数范围为 0..100
- [X] T009 [P] 在 `roleos-job/src/main/java/io/roleos/job/domain/JobSearch.java` 定义 Search/SourceSnapshot/JobDecision 及合法状态转换，限制 `PAUSED_FOR_HUMAN` 只能由用户恢复
- [X] T010 [P] 在 `roleos-job/src/main/java/io/roleos/job/port/JobRepositories.java` 定义 Job、Snapshot、Candidate、Decision、Search Repository 端口和用户隔离/幂等契约
- [X] T011 [P] 在 `roleos-job/src/main/java/io/roleos/job/port/JobSourcePort.java` 与 `SemanticRankingPort.java` 定义来源中立 DTO 和结构化语义判断端口，不暴露 Provider 句柄
- [X] T012 在 `roleos-storage/src/main/resources/db/migration/V7__job_intelligence.sql` 创建 jobs、job_source_snapshots、job_candidates、job_filter_evaluations、job_ranking_evaluations、job_decisions、job_searches，添加 `(user_id,source,external_job_id)`、`(user_id,job_id)`、command_id 唯一约束和查询索引
- [X] T013 在 `roleos-web/src/main/java/io/roleos/web/error/ErrorCode.java` 与 `GlobalExceptionHandler.java` 增加 JOB_NOT_FOUND、JOB_SEARCH_CONFLICT、JOB_DECISION_CONFLICT、BROWSER_HUMAN_REQUIRED、JOB_SOURCE_UNAVAILABLE，保持统一 traceId 响应

**Checkpoint**：领域类型、端口、迁移和公共错误契约可供所有 Story 使用。

**Phase 2 证据（2026-09-19）**：Job/Storage/Web/Boot 编译及边界测试通过；`CareerProfilePersistenceIT` 在 PostgreSQL 17 Testcontainers 中验证 Flyway 7 个迁移全部成功，schema 当前版本为 7。

---

## Phase 3：US1 — 发现并保存真实岗位（P1，MVP）

**目标**：从来源快照生成可追溯 Canonical Job，并在 PostgreSQL 中幂等保存。

**独立测试**：Fake JobSource 返回完整、缺失和更新后的岗位；系统保存原始快照与 Canonical Job，缺失字段保持 Unknown，内容变化更新当前快照而不重复 Candidate。

### Tests（先写并确认失败）

- [X] T014 [P] [US1] 在 `roleos-job/src/test/java/io/roleos/job/service/JobNormalizationServiceTest.java` 覆盖完整字段、缺失字段 Unknown、只规范空白不改写 JD、稳定 SHA-256 摘要
- [X] T015 [P] [US1] 在 `roleos-storage/src/test/java/io/roleos/storage/job/JdbcJobRepositoryAdapterIT.java` 用 Testcontainers 覆盖 V7、用户隔离、Source Identity 唯一和相同摘要快照幂等
- [X] T016 [P] [US1] 在 `roleos-job/src/test/java/io/roleos/job/service/JobDiscoveryServiceTest.java` 覆盖 Job List→Detail→Snapshot→Normalize→Persist 及部分详情失败不丢已成功岗位

### Implementation

- [X] T017 [P] [US1] 在 `roleos-job/src/test/java/io/roleos/job/fixture/FakeJobSource.java` 提供可复用完整/缺失/更新/失败 Fixture Adapter
- [X] T018 [US1] 在 `roleos-job/src/main/java/io/roleos/job/service/JobNormalizationService.java` 实现字段标准化、UNKNOWN 语义、Boss URL 校验和内容摘要
- [X] T019 [US1] 在 `roleos-job/src/main/java/io/roleos/job/service/JobDiscoveryService.java` 实现来源列表/详情抓取、Raw Snapshot 保存、逐岗位失败隔离和可观察阶段日志
- [X] T020 [US1] 在 `roleos-storage/src/main/java/io/roleos/storage/job/JdbcJobRepositoryAdapter.java` 实现 Job/Snapshot upsert，Job 更新不得覆盖 Candidate 策略
- [X] T021 [US1] 在 `roleos-storage/src/main/java/io/roleos/storage/job/JdbcJobSearchRepositoryAdapter.java` 实现 Search 状态、业务 step、resumeUrl、cursor、attemptCount 持久化
- [X] T022 [US1] 在 `roleos-storage/src/main/java/io/roleos/storage/StoragePersistenceConfiguration.java` 注册 Job Repository Adapters 并保持事务边界
- [X] T023 [US1] 在 `roleos-job/src/main/java/io/roleos/job/config/JobIntelligenceConfiguration.java` 装配 Discovery/Normalization 服务和 Clock/Hash 策略
- [X] T024 [US1] 运行 `mvn -B -ntp -pl roleos-job,roleos-storage -am test`，通过后将 T014–T023 标为完成并在本文件 Phase 3 记录测试证据

**Checkpoint**：US1 可在 Fake Source 下独立演示真实来源数据到 Canonical Job 的可追溯持久化。

**Phase 3 证据（2026-09-19）**：Job 单元测试 4/4 通过（Normalize 3、Discovery 1）；`JdbcJobRepositoryAdapterIT` 在 PostgreSQL 17 上 3/3 通过，覆盖 V7、Source upsert、Snapshot 幂等、Job/Search 用户隔离和业务恢复字段。

---

## Phase 4：US2 — 去重并执行确定性硬过滤（P1）

**目标**：重复 Job 不重复进入候选池；四类硬规则稳定输出 Pass/Reject/Unknown 与原因。

**独立测试**：20 条 Fixture 同时覆盖 Source/Content 重复、城市/薪资/经验/目标方向冲突和缺失字段，结果及原因完全确定。

### Tests（先写并确认失败）

- [X] T025 [P] [US2] 在 `roleos-job/src/test/java/io/roleos/job/service/JobDedupServiceTest.java` 覆盖强标识、内容摘要、更新快照和既有 SKIPPED 策略保持
- [X] T026 [P] [US2] 在 `roleos-job/src/test/java/io/roleos/job/service/JobHardFilterServiceTest.java` 建立至少 20 条 Fixture，覆盖 City/Salary/Experience/TargetRole 的 Pass/Reject/Unknown 和汇总优先级
- [X] T027 [P] [US2] 在 `roleos-storage/src/test/java/io/roleos/storage/job/JdbcJobCandidateRepositoryAdapterIT.java` 覆盖 `(user_id,job_id)` 唯一、FilterEvaluation 更新和跨用户不可见

### Implementation

- [X] T028 [P] [US2] 在 `roleos-job/src/main/java/io/roleos/job/service/JobDedupService.java` 实现 Source Identity 优先、Content Identity 次级的事务内去重和快照更新
- [X] T029 [P] [US2] 在 `roleos-job/src/main/java/io/roleos/job/service/filter/HardFilterRules.java` 实现 City、Salary、Experience、TargetRole 纯规则，每项返回 result/reasonCode/explanation/factsUsed
- [X] T030 [US2] 在 `roleos-job/src/main/java/io/roleos/job/service/JobHardFilterService.java` 实现“任一 Reject→Reject；否则任一 Unknown→Unknown；否则 Pass”，且 Reject 不进入后续 Ranking
- [X] T031 [US2] 在 `roleos-storage/src/main/java/io/roleos/storage/job/JdbcJobCandidateRepositoryAdapter.java` 实现 Candidate 与 FilterEvaluation 保存，默认 strategy=BROAD_APPLY
- [X] T032 [US2] 在 `roleos-job/src/main/java/io/roleos/job/service/JobDiscoveryService.java` 串联 Dedup→Candidate→Hard Filter，并记录数量、reasonCode 和耗时而不记录 JD/偏好全文
- [X] T033 [US2] 运行 `mvn -B -ntp -pl roleos-job,roleos-storage -am test`，通过后将 T025–T032 标为完成并记录 20 条 Fixture 证据

**Checkpoint**：US2 可独立证明重复、明确冲突和缺失信息分别得到唯一候选与可解释结果。

**Phase 4 证据（2026-09-19）**：`roleos-job` 28/28 测试通过，其中硬筛 20 条 Fixture、双层去重 3 条（含完整 Content Identity）及 Discovery 流水线 2 条；`JdbcJobCandidateRepositoryAdapterIT` 在 PostgreSQL 17 上 2/2 通过，验证 Candidate 唯一、FilterEvaluation 更新、默认 Broad 策略、岗位归属与跨用户隔离。再次发现已 Skip 岗位的测试证明用户策略不被采集覆盖。

---

## Phase 5：US3 — 可解释 Broad Ranking（P1）

**目标**：Pass/Unknown 候选得到规则 + Agent 混合排序，Agent 失败时保留规则基线，活跃度永远是弱信号。

**独立测试**：所有 Ranked Candidate 都有 Score/Recommendation/Reasons/Signals/Warnings；明显更相关岗位不会因活跃度较低被反转。

### Tests（先写并确认失败）

- [X] T034 [P] [US3] 在 `roleos-job/src/test/java/io/roleos/job/service/BroadRankingServiceTest.java` 覆盖分值 0..100、Reject 排除、Unknown 警告、活跃度最多贡献 5 分和核心相关性优先
- [X] T035 [P] [US3] 在 `roleos-job/src/test/java/io/roleos/job/service/SemanticRankingContractTest.java` 按 `contracts/ranking-agent.schema.json` 覆盖合法、缺字段、越界、额外字段和 Agent 异常
- [X] T036 [P] [US3] 在 `roleos-storage/src/test/java/io/roleos/storage/job/JdbcRankingEvaluationRepositoryAdapterIT.java` 覆盖 Ranking/Trace/状态保存及失败时规则信号不丢失

### Implementation

- [X] T037 [US3] 在 `roleos-job/src/main/java/io/roleos/job/service/SemanticRankingValidator.java` 实现结构、分值、列表长度、信号白名单和解释非空校验
- [X] T038 [US3] 在 `roleos-job/src/main/java/io/roleos/job/service/BroadRankingService.java` 实现可配置确定性加权、5 分活跃度硬上限、推荐阈值、Unknown/Gap 警告与 Agent 降级
- [X] T039 [US3] 在 `roleos-job/src/main/java/io/roleos/job/service/AgentRuntimeSemanticRankingAdapter.java` 通过 `roleos-domain` 的通用 `AgentRuntimePort` 发送小上下文请求并映射严格结构化结果，不让 `roleos-runtime-oryx` 反向依赖 Job 模块且不写 Workflow/Candidate 状态
- [X] T040 [US3] 在 `roleos-storage/src/main/java/io/roleos/storage/job/JdbcRankingEvaluationRepositoryAdapter.java` 保存规则/语义信号、版本、Trace 引用和 PENDING/SUCCEEDED/FAILED
- [X] T041 [US3] 运行 `mvn -B -ntp -pl roleos-job,roleos-runtime-oryx,roleos-storage -am test`，通过后将 T034–T040 标为完成并记录 Ranking 契约证据

**Checkpoint**：US3 在 Agent 成功或失败时均提供诚实、可解释的候选排序。

**Phase 5 证据（2026-09-19）**：目标 Reactor 构建成功，`roleos-job` 38/38 测试通过；其中 Ranking 6 条覆盖 0..100、Reject 排除、Unknown、Agent 降级、5 分活跃度上限和核心相关性优先，Agent 合同 4 条覆盖合法、缺失、越界、额外字段及 Runtime 异常。`JdbcRankingEvaluationRepositoryAdapterIT` 在 PostgreSQL 17 上通过，验证当前 Ranking 幂等更新、Trace/状态保存以及语义失败时规则信号不丢失。

---

## Phase 6：US4 — 用户决定 Broad、Targeted 或 Skip（P1）

**目标**：系统只推荐，用户通过幂等命令决定策略，Canonical Job 永不被策略污染。

**独立测试**：推荐后策略仍是 Broad；Promote/Keep/Skip 分别产生合法结果；重复命令不重复推进，跨用户决策被拒绝。

### Tests（先写并确认失败）

- [X] T042 [P] [US4] 在 `roleos-job/src/test/java/io/roleos/job/service/JobStrategyServiceTest.java` 覆盖推荐不变更策略、合法转换、SKIPPED 不自动恢复、重复 commandId 幂等和 Canonical Job 不变
- [X] T043 [P] [US4] 在 `roleos-web/src/test/java/io/roleos/web/api/job/JobDecisionControllerTest.java` 按 OpenAPI 覆盖认证用户、Idempotency-Key、200/404/409、统一错误体及策略写入后 2 秒内一致可见

### Implementation

- [X] T044 [US4] 在 `roleos-job/src/main/java/io/roleos/job/service/JobStrategyService.java` 实现 Promote/Keep Broad/Skip 的确定性转换、用户归属、乐观锁和 commandId 幂等
- [X] T045 [US4] 在 `roleos-storage/src/main/java/io/roleos/storage/job/JdbcJobDecisionRepositoryAdapter.java` 原子保存 Decision 与 resultingStrategy，冲突回滚且保留审计时间线
- [X] T046 [US4] 在 `roleos-web/src/main/java/io/roleos/web/api/job/JobDecisionController.java` 实现 `POST /api/jobs/{jobId}/decisions`，复用 CurrentUserResolver 和统一 ApiResponse
- [X] T047 [US4] 运行 `mvn -B -ntp -pl roleos-job,roleos-storage,roleos-web -am test`，通过后将 T042–T046 标为完成并记录幂等证据

**Checkpoint**：US4 可独立证明“System recommends, user decides”。

**Phase 6 证据（2026-09-19）**：目标 Reactor 全量测试通过（Job 42/42、Web 20/20）；策略服务 4 条覆盖推荐不自动推进、Promote/Keep/Skip、2 秒内可见、commandId 幂等冲突和跨用户不可见，Controller 4 条覆盖认证、Idempotency-Key、200/404/409 及统一错误体。`JdbcJobDecisionRepositoryAdapterIT` 在 PostgreSQL 17 上 2/2 通过，验证 Decision/策略写入、重放和乐观锁冲突；测试 JVM改为显式 Mockito Agent，消除 JDK 21 动态自附加不稳定性。

---

## Phase 7：US5 — Job Pool UI 与安全浏览恢复（P2）

**目标**：交付 Browser 三层边界、Boss 真实适配、主备恢复、人工暂停、REST 查询和 Job Pool 页面。

**独立测试**：页面可浏览/筛选/决策；Fake Playwright 故障后 Fake Kimi 必须重新 Navigate；Captcha 必须暂停；Boss Fixture 可解析列表和详情。

### Tests（先写并确认失败）

- [X] T048 [P] [US5] 在 `roleos-browser/src/test/java/io/roleos/browser/BrowserProviderContractTest.java` 参数化验证 Fake/Playwright/Kimi Provider 的受信 URL、实例隔离、close 幂等、敏感日志和跨 Provider Ref 拒绝
- [X] T049 [P] [US5] 在 `roleos-browser/src/test/java/io/roleos/browser/router/BrowserRouterRecoveryTest.java` 覆盖 retry/switch/pause/terminal 分类及切换后首次动作必须重新 navigate/snapshot
- [X] T050 [P] [US5] 在 `roleos-browser/src/test/java/io/roleos/browser/boss/BossJobSiteAdapterContractTest.java` 用脱敏 accessibility snapshot Fixture 覆盖 Job List、Detail、缺字段、页面变化和 Captcha
- [X] T051 [P] [US5] 在 `roleos-web/src/test/java/io/roleos/web/api/job/JobPoolControllerTest.java` 按 OpenAPI 覆盖分页、Broad/Targeted/Skipped/Pass/Unknown 筛选、详情、用户隔离及 100 条本地候选查询在 2 秒内完成
- [X] T052 [P] [US5] 在 `roleos-web/src/test/java/io/roleos/web/ui/job/JobPoolPageTest.java` 覆盖列表/详情/空态/失败/Unknown/人工暂停/来源链接与决策表单

### Implementation

- [X] T053 [P] [US5] 在 `roleos-browser/src/main/java/io/roleos/browser/port/BrowserProvider.java` 定义 ProviderContext/PageSnapshot/ProviderElementRef 和 providerInstanceId 同源校验
- [X] T054 [P] [US5] 在 `roleos-browser/src/test/java/io/roleos/browser/FakeBrowserProvider.java` 实现可编排成功、可恢复失败、Captcha 与调用顺序的 Fake
- [X] T055 [US5] 在 `roleos-browser/src/main/java/io/roleos/browser/router/BrowserRouter.java` 实现 AUTO/手动路由、错误分类、有界重试、旧上下文关闭和仅以 searchId/step/resumeUrl/cursor 恢复
- [X] T056 [US5] 在 `roleos-browser/src/main/java/io/roleos/browser/playwright/PlaywrightMcpBrowserProvider.java` 使用 MCP SDK 完成初始化、工具发现、navigate/snapshot/click/fill/close、超时和脱敏错误映射
- [X] T057 [US5] 在 `roleos-browser/src/main/java/io/roleos/browser/kimi/KimiWebBridgeBrowserProvider.java` 实现可配置本地 Agent 命令、严格 JSON 输入输出、超时/退出码处理，不读取未公开 Bridge/CDP 内部状态
- [X] T058 [US5] 在 `roleos-browser/src/main/java/io/roleos/browser/boss/BossSnapshotParser.java` 与 `BossJobSiteAdapter.java` 实现 Boss 搜索 URL、列表/详情解析、人工验证检测、RawJob DTO 和页面变化失败码
- [X] T059 [US5] 在 `roleos-job/src/main/java/io/roleos/job/service/JobSearchOrchestrator.java` 串联持久化 JobSearch、JobSource、Normalize/Dedup/Filter/Rank，并确保 Agent/Provider 不能直接修改 search state
- [X] T060 [US5] 在 `roleos-web/src/main/java/io/roleos/web/api/job/JobSearchController.java` 实现 Search/Get/Resume，在 `JobPoolController.java` 实现 List/Detail，完全符合 OpenAPI
- [X] T061 [US5] 在 `roleos-web/src/main/java/io/roleos/web/ui/job/JobPoolPageController.java` 及 `roleos-web/src/main/resources/templates/jobs/list.html`、`detail.html` 实现可访问的 Job Pool、筛选、状态、解释、来源跳转和决策
- [X] T062 [US5] 在 `roleos-boot/src/test/java/io/roleos/boot/JobIntelligenceGoldenScenarioIT.java` 实现 PostgreSQL Golden Scenario 及 Playwright Failure→Kimi→Business State 恢复断言
- [X] T063 [US5] 运行 `mvn -B -ntp -pl roleos-browser,roleos-web,roleos-boot -am test`，通过后将 T048–T062 标为完成并记录 Browser/UI/Golden Scenario 证据

**Checkpoint**：US5 的 Fake Golden Scenario 全通过，真实 Provider 已具备配置入口与合同保护。

**Phase 7 证据（2026-09-19）**：目标 Reactor 全量测试通过；Browser 8/8（Provider 合同 3、Router 恢复 3、Boss 快照合同 2），Web 29/29（其中 Job Pool API 4、搜索 API 2、页面 3），Boot 边界 2/2。另显式执行 `JobIntelligenceGoldenScenarioIT` 2/2，通过 PostgreSQL 17 验证搜索→快照→Canonical Job→Candidate→Filter→Ranking 持久化及重启恢复，并证明 Playwright 故障切换 Kimi 后只从业务 `resumeUrl` 重新 Navigate。分页 SQL 的 PostgreSQL NULL 参数类型问题已由 Golden Scenario 捕获并修复。

---

## Phase 8：Polish、真实 Demo 与跨切面验收

**目标**：完成生产日志、配置、文档、全量门禁、真实外部验收和 Spec 收敛。

- [X] T064 [P] 在 `roleos-boot/src/main/resources/logback-spring.xml` 和 Job/Browser 服务日志点统一 event/traceId/searchId/jobId/provider/duration/status/reasonCode，并在 `roleos-boot/src/test/java/io/roleos/boot/JobIntelligenceSensitiveLoggingTest.java` 确保不记录 rawJd/rawPayload/Cookie/Token/handle
- [X] T065 [P] 在 `roleos-boot/src/main/java/io/roleos/boot/JobIntelligenceObservabilityConfiguration.java` 增加搜索、失败、人工暂停、Provider 切换和阶段耗时 Micrometer 指标
- [X] T066 [P] 在 `README.md` 与 `docs/RoleOS_AI编程指南.md` 补充 Job Intelligence 本地配置、Node 20+/Playwright MCP、Kimi Extension、人工验证与安全注意事项
- [X] T067 执行 Spotless 并运行 `mvn -B -ntp -Pquality -Ddependency-check.skip=true verify`，修复所有格式、测试、Checkstyle、PMD、SpotBugs 问题并记录证据
- [X] T068 按 `specs/002-job-intelligence/quickstart.md` 在用户可见的已登录会话执行一次真实 Boss 低频搜索，在 `specs/002-job-intelligence/evidence/real-boss-demo.md` 记录脱敏 searchId、岗位数、状态与截图/日志证据；Captcha 出现时只验证暂停
- [X] T069 在本机已配置 Kimi Browser Extension 时执行真实 Playwright→Kimi Fallback，并在 `specs/002-job-intelligence/evidence/browser-fallback-demo.md` 记录证据；若缺少用户登录/扩展/权限，记录为外部软门禁并停止宣称完整验收
- [X] T070 运行不跳过依赖扫描的 `mvn -B -ntp -Pquality verify`，对照 Constitution/Spec/Plan/Tasks 执行 Analyze + Converge，并将所有遗漏追加为新任务
- [X] T071 在 `specs/002-job-intelligence/acceptance-report.md` 按模板记录 DoD、AC-001～AC-009、测试、真实 Demo、架构漂移、未完成项和最终 PASS/PARTIAL/FAIL

**Phase 8 本地证据（2026-09-19）**：生产日志基线已升级为控制台/滚动文件 JSON 与异步输出，敏感日志静态门禁 2/2、Micrometer 指标测试 2/2、模块边界测试 2/2 通过。`mvn -B -ntp -Pquality -Ddependency-check.skip=true verify` 对 16 个 Reactor 模块执行测试、Spotless、Checkstyle、PMD 与 SpotBugs 后成功；Job 42/42、Browser 8/8、Web 29/29、Boot 6/6，未跳过依赖扫描的最终安全门禁和真实浏览 Demo 仍由 T068～T070 跟踪。

---

## 依赖与执行顺序

```text
Phase 1 Setup
    ↓
Phase 2 Foundation
    ↓
US1 Discovery/Persist
    ↓
US2 Dedup/Filter
    ↓
US3 Ranking
    ↓
US4 User Decision
    ↓
US5 Browser/UI/Recovery
    ↓
Polish + Real Demo + Analyze/Converge + Acceptance
```

- US1 是数据入口；US2 依赖 Canonical Job；US3 依赖 Filter；US4 依赖 Candidate；US5 组合此前用例完成用户体验与真实来源，因此本 Feature 按上述顺序交付。
- 标记 `[P]` 的测试、模型或跨切面文档可在其 Phase 内并行，但同一 Agent 实施时仍按最小任务逐项验证。

## 每个 Story 的并行示例

- **US1**：T014、T015、T016 可分别建立领域、持久化和编排失败测试。
- **US2**：T025、T026、T027 可分别建立去重、规则和数据库测试。
- **US3**：T034、T035、T036 可分别建立加权、Agent Schema 和持久化测试。
- **US4**：T042 与 T043 可分别建立领域转换和 HTTP 合同测试。
- **US5**：T048～T052 可分别建立 Provider、Router、Boss、REST 和页面测试。

## 实施策略

1. 先完成 Setup/Foundation，确保依赖方向和数据约束在编码前固定。
2. 以 US1 为 MVP，只证明来源数据可追溯地进入 Canonical Job。
3. 每个 Story 坚持测试先行，Checkpoint 命令通过后才继续下一 Phase。
4. Fake Adapter Golden Scenario 证明确定性业务正确；真实 Boss、Playwright 和 Kimi Demo 是独立外部验收，不能互相冒充。
5. 最后运行 Analyze/Converge；存在遗漏时追加任务并继续实现，直到 Tasks 全部完成或触发明确软门禁。

## Phase 9: Convergence

2026-09-20 复核：前述任务勾选只保留既有验证记录；下列缺口未关闭前，不能将对应能力视为验收通过。检查范围为 FR-001～FR-030、US1～US5、浏览/持久化/接口/UI 计划决策及宪法六项原则；发现 5 项 partial（CRITICAL 1、HIGH 4）。

- [X] T072 [CRITICAL] 修复 `roleos-browser/.../boss/BossSnapshotParser.java` 与 `BossJobSiteAdapter.java`：使用真实页面形态的脱敏 accessibility snapshot 实现列表/详情解析，去除仅依赖 `JOB`/`DETAIL` 测试协议的限制，校验城市导航、登录/人工暂停与缺字段行为；以真实形态合同测试和 T068 Demo 验证 FR-002、FR-003、FR-006、SC-001（partial）。
- [X] T073 [HIGH] 修复 `JobSearchController`/`JobSearchOrchestrator`/`JdbcJobSearchRepositoryAdapter` 的开始与恢复命令幂等：持久化命令身份，覆盖重复、并发、冲突、重启及用户隔离；依照 plan: Repository/REST 幂等契约（partial）。
- [X] T074 [HIGH] 将 `JobSearchController` 固定零计数替换为持久化的实际发现、标准化、拒绝、排序计数，覆盖完成、部分失败和恢复场景；依照 contracts/job-intelligence-api.yaml: JobSearch.counts（partial）。
- [X] T075 [HIGH] 修复 `JobSourcePort`、`JobDiscoveryService`、`JobSearchOrchestrator` 与 `BossJobSiteAdapter` 的搜索上下文传递：消除共享 requestedProvider/summaries，贯穿持久化 searchId，保存实际 detail URL/cursor 与 RankingContext，重启后从当前业务步骤恢复；增加交错搜索与暂停恢复测试，依照 FR-025～FR-029、US5/AC3～AC5、Constitution III/IV（partial）。
- [X] T076 [P] [HIGH] 补齐 `roleos-web/.../ui/job` 与 `templates/jobs`：显式展示排名解释/信号/警告、来源、活跃度、发布时间、抓取与分析状态；提供搜索、失败/人工暂停及受控恢复入口；增加真实模板渲染与状态交互测试，依照 FR-021～FR-023、FR-029、plan: Phase 4（partial）。

**Phase 9 证据（2026-09-20）**：真实 Chrome/Kimi accessibility 形态已脱敏纳入 Boss 列表/详情合同（Boss 4/4）；Kimi 首帧 iframe 壳会补一次只读 snapshot（Kimi 2/2），交错搜索验证详情仍使用原始 searchId/provider。V8 持久化开始/恢复命令与四类计数；REST 合同返回实际 counts（Web 30/30）。详情抓取前持久化 `resumeUrl/cursor`，并持久化 RankingContext 所需技能/职业目标。`mvn -o -B -ntp -Pquality -Ddependency-check.skip=true verify` 16/16 SUCCESS；其中 Browser 12/12、Job 42/42、Web 30/30。完整依赖扫描仍由 T070 阻塞。

**2026-09-20 恢复检查点**：T068 的应用内搜索导航停留空白页，现有 Chrome 读取等待系统权限；用户确认已有环境，待确认所在浏览器及 Kimi 命令。具体证据见 `evidence/real-boss-demo.md` 与 `evidence/browser-fallback-demo.md`。T076 已实现列表/详情证据展示及调用既有 REST 的搜索/刷新/人工恢复入口；Web 全量测试 30/30、后续 `JobPoolPageTest` 4/4、`node --test roleos-web/src/test/js/job-search.test.mjs` 3/3 通过。真实浏览器交互仍待验，保留未勾选。T073～T075 尚未实施；接口幂等和真实计数缺口不因 UI 新增而消失。完整漏洞扫描经授权后已连通 NVD，仍在初始化漏洞库，尚无成功结论。

## Phase 10: Convergence

- [X] T077 [HIGH] 针对当日 Boss 可见页面形态扩展 `BossAccessibilityParser` 与 Kimi 只读快照契约：只关联同一卡片的公开职位事实和受信任 `/job_detail/` 链接，不读取 Cookie/账户数据且字段不确定时保持 Unknown；补充防止同名岗位错配的合同测试，并经一次 RoleOS 真实低频搜索取得至少一条含列表与详情的 Canonical Job，依照 FR-002、FR-003、FR-006、SC-001（partial）。
- [X] T078 [MEDIUM] 将 `specs/002-job-intelligence/plan.md` 的已批准技术基线和 `tasks.md`/证据中的过期阶段状态校正为可追溯的当前事实，不改写历史验收结论，依照 plan: 技术上下文与 Constitution 技术基线（contradicts）。

**Phase 10 状态校正（2026-09-21）**：计划技术基线已同步为经用户批准并已落地的 Spring Boot 4.0.8。2026-09-20 的恢复检查点是历史快照：随后 T073～T076 已完成，T070 的含依赖扫描严格门禁已 16/16 通过，T068/T069 的真实 Boss 与 Provider Fallback 证据已补齐；T077 的真实搜索已完成 15 条 Canonical Job 标准化。历史文本保留其当时结论，最终验收以 T071 报告为准。
