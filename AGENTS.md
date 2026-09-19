# RoleOS 智能体执行规范

## 语言与沟通

- 默认使用中文与用户沟通，并优先生成中文文档、注释、需求说明和技术说明。
- 代码标识符、协议字段、框架术语和业内通用技术名词可保留英文；首次出现时可附中文解释。
- 用户明确指定其他语言时，以用户要求为准。

## 事实来源

进行架构、跨模块、工作流、集成或数据模型变更前，必须先阅读相关章节：

- `docs/RoleOS需求文档.md`：产品范围和领域定义；
- `docs/RoleOS技术方案.md`：架构和模块边界；
- `docs/RoleOS_AI编程指南.md`：交付流程和工程纪律；
- `.specify/memory/constitution.md`：项目章程（章程完成并批准后具有最高优先级）。

这些文档是实现约束。实现过程中不得擅自重设计产品或架构；若任务与上述文档冲突，应暂停并报告冲突，等待人工决策。

## 产品目标与真实性红线

RoleOS 是面向 AI 应用开发、AI Agent 开发等技术岗位求职者的 AI Career Agent。V1 的核心闭环为：

`岗位 → JD 分析 → 经历匹配 → Gap 分析 → 经历深挖 → 项目升级 → Evidence → 简历/面试故事 → 人工确认 → 投递 → 结果`

- 严禁虚构经历、用户、客户、收入、生产规模、性能指标、历史项目或其他职业事实。
- 必须清晰区分 `User Fact`、`Agent Inference`、`Designed Scenario` 和 `Upgraded Capability`。
- `Portfolio Project` 不等于 `Production Experience`；设计场景不等于真实公司场景。
- 坚持 **Upgrade Before Rewrite**：真实 Skill Gap 必须先完成项目升级、测试/评测和证据沉淀，才可以支持对应 Claim。
- 重要决策和推荐必须可解释、可追溯。

## 架构规范

- 除非经过批准的计划变更，技术基线固定为 Java 21、Spring Boot 3.x、Maven 多模块、PostgreSQL、Spring Data JPA 和 Flyway。
- V1 使用模块化单体。未经明确架构变更，不得引入微服务、Kafka、Kubernetes、Redis Workflow Engine、Temporal、Graph DB、高级向量基础设施或多智能体编排框架。
- RoleOS 自己拥有 Career Domain 和持久化 Workflow；OryxOS 仅作为 Agent Runtime/Harness。原则：**借 Runtime，不借业务**。
- Domain 模块只能依赖 Port，不能依赖具体的 OryxOS、Playwright、Kimi 或 Codex 实现类。仅 `roleos-runtime-oryx` 可以直接依赖 OryxOS Runtime API。
- OryxOS/LLM、浏览器、Coding Agent、评测、简历渲染和岗位来源等外部能力必须通过 Port/Adapter 接入。
- 新外部能力优先按以下顺序实现：`Port → Fake Adapter → Contract Test → Real Adapter`。
- 浏览器分层固定为：`Career Domain → JobSiteAdapter → BrowserRouter → BrowserProvider`。Domain 代码不得出现 Playwright Page、CSS Selector、XPath 或 Kimi WebBridge Handle。
- 明确区分 `OpenAPI`（RoleOS 对外 REST 接口文档）与 `OpenAI API`（模型推理服务）。职业判断走
  `AgentRuntimePort → OryxOS Adapter`；工程升级走 `CodingAgentPort → CodexAdapter`。即使两者
  使用同一模型提供商，也不得共享工具权限、成本/Trace 记录或业务职责。

## Workflow、事实与 Agent 规范

- 落实 **Workflow Outside, Agent Inside**：Java/Workflow 负责持久化状态、状态转换、幂等、重试和确定性规则；Agent 仅返回结构化判断、建议和计划。
- Agent 绝不能直接设置 `currentStage` 或修改 Workflow State。唯一合法路径是：`Agent Decision → Workflow Rule → Persisted Transition`。
- Career Workflow 必须持久化到 PostgreSQL，并能够在服务重启后从 `WAITING_USER` 和 `WAITING_APPROVAL` 恢复。
- 落实 **Agent for Judgment, Code for Rules**：筛选、排序计算、去重、状态转换等稳定逻辑必须使用确定性的 Java 代码，而非 LLM。
- Career Database 保存结构化事实：Experience、Project、Skill、Capability、Job、Requirement、Gap、Claim、Evidence、Workflow、Resume、Application 和 Outcome。Agent Memory 仅保存偏好、对话上下文和交互上下文。
- 所有 Agent 输出在持久化前必须通过结构化 Schema 校验。未经用户确认或 Evidence 支持的 Agent Inference 不得成为用户事实。
- 关键 Career Fact 和 Claim 必须具备 Provenance，例如 `USER_CONFIRMED`、`SOURCE_CODE`、`PROJECT_UPGRADE`、`AGENT_INFERENCE`、`DESIGNED_SCENARIO`。
- Resume Compiler 只允许使用 `VERIFIED` 或 `SUPPORTED` 的 Claim，必须排除 `UNSUPPORTED`。

## 外部操作与人工确认

- Experience Confirmation、重大 Project Upgrade、最终 Resume、正式 Application、验证码/人工验证和未知外部副作用，均必须经过 Human Approval。
- Codex 返回 `completed` 不表示 Project Upgrade 完成。必须独立验证 Build、Test、Evaluation 和预期 Artifact，再记录为 `PASS`、`PARTIAL` 或 `FAIL`。
- Project Upgrade 必须使用独立 Git Worktree/Branch，绝不能直接修改用户的主分支；升级分支使用 `roleos/upgrade/{upgradeId}`。
- Browser Provider 可以切换，但不能迁移 `ElementRef`、DOM Snapshot、Tab Handle 或 Browser Session。必须从 `BrowserTaskState` 重新导航和抓取快照后恢复业务步骤。
- `SUBMIT_APPLICATION`、发送消息、撤回投递等副作用操作必须具备人工确认和幂等性。若无法确认操作是否已生效，应暂停等待人工处理；不得盲目重试或在 Provider 切换后重复执行。
- Broad Job 必须走低成本路径；不得对所有 Broad Job 自动进行经历深挖、项目升级或岗位定制简历。

## 实施流程

- V1 功能开发遵循 Spec Kit：`constitution → spec → plan → tasks → implementation → tests/analyze → demo`。小型、隔离的修复可使用清晰的任务契约。
- 一次只处理一个小而可独立验收的 Task。编码前先阅读相关实现，并列出最小受影响模块和文件。
- 未经任务要求，不得创建平行 Service 或进行无关重构。若小任务突然需要修改大量无关文件，应暂停并重新评估任务范围或架构漂移。
- 每个 Task 都必须新增或更新合适的测试。确定性行为优先测试/验收条件驱动；Agent 行为使用 Fixture 和结构化输出契约测试，而不是断言逐字文本。
- 按需覆盖 Unit、Repository/Testcontainers、Agent Contract、Adapter Contract、Workflow Recovery 和 Golden Scenario E2E 测试。
- 声明完成前必须运行相关 Maven 测试。一个 Story 完成还需要测试、架构 Analyze、Demo、日志/Trace 可见性以及人工 Review 均满足要求。
- 每个 Story 后，对照 Constitution、Spec、Plan、Tasks 和代码检查架构漂移。不得通过修改 Constitution 来掩盖实现冲突。

## Spec Kit 连续执行

- 当 Feature Spec 已通过质量清单后，后续默认由 `roleos-sdd-autopilot` 推进 `clarify → plan → tasks → analyze → implement → converge → 验收`；用户无需逐条输入各 Skill 命令。
- 实现必须从 `tasks.md` 中首个未完成的任务恢复，严格按 Phase 与依赖执行；每项仅在实现、对应测试和适用质量门禁通过后标记为 `[X]`。
- 用户指定某个恢复任务时，该任务仅确定恢复锚点，不构成单次执行上限；完成后必须自动继续下一个依赖已满足的未完成任务，直到触发软门禁或 Feature 完成。
- 在连续执行期间，任务/阶段进度只能通过 `commentary` 汇报；不得因“完成一个 Task”“完成一个 Phase”或“测试通过”主动结束持久 Goal。最终回复仅允许在 Feature 全部完成、触发软门禁需要用户决策，或用户明确要求暂停时发送。
- `speckit-implement` 只定义单次执行流程，不提供后台调度。需要无人手动逐条触发的交付，必须以 `roleos-sdd-autopilot` 启动：优先在同一活动回合内循环处理下一个可执行任务；运行环境支持 Goal 时，允许由同一持久 Goal 跨 turn 自动恢复。若没有 Goal 或宿主调度器，发送 final 后不得声称仍会自动继续。
- 不得因普通编译错误、格式错误、可恢复的依赖缓存问题或可由既有文档决定的实现细节停止。应先自行诊断、修复并重试，再报告结果。
- 只有以下软门禁可以暂停并询问用户：需求/范围存在实质歧义；与宪法或已批准 Spec/Plan 冲突；需要新增公共概念、第三方依赖、数据表、配置键或 REST 路径且计划未列明；不可逆删除；外部副作用；权限、密钥、费用或安全策略变更。
- 遇到软门禁时，必须说明冲突证据、可选方案、影响与推荐选项；不得自行越过。用户回复后从首个未完成任务继续。
- 每阶段结束必须写入可验证证据：已完成任务、测试/构建结果、未完成任务、阻塞原因和下一步。未完成验证不得宣称 Feature 完成。

## V1 范围纪律

- 优先跑通真实 Golden Scenario，而非追求模块数量、可选集成或高级架构。
- V1 不包含无人值守批量投递、绕过验证码、自研 Browser Runtime、自研 Coding Agent、通用 Workflow Engine、完整 Career Knowledge Graph 或复杂 Multi-Agent Network。
- 优先接入一个真实 Job Source，并使用结构化数据/全文检索；在此之前不扩展多 Provider 或高级检索能力。

## 完成检查清单

在标记 Task 完成前，逐项确认：

1. 行为符合任务和事实来源文档。
2. 没有绕过事实、Claim、人工确认或架构边界。
3. 相关测试通过，Agent/Adapter 已具备必要契约覆盖。
4. 变更最小、可解释、可观测，且没有用 TODO 替代核心逻辑。
5. 外部副作用和项目升级已完成要求的独立验证和人工确认。
