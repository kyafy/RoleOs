# 实施计划：Job Intelligence（岗位智能）

**分支**：`002-job-intelligence` | **日期**：2026-09-19 | **规格**：[spec.md](spec.md)

**输入**：`specs/002-job-intelligence/spec.md`

## 摘要

在现有 Java 21 / Spring Boot 模块化单体内完成 Job Intelligence：以 `roleos-job` 承载 Canonical Job、JobCandidate、确定性过滤、去重和可解释 Ranking；以 `roleos-browser` 实现 `JobSiteAdapter → BrowserRouter → BrowserProvider` 及 Boss/Playwright MCP/Kimi Browser Extension 适配；以 PostgreSQL/Flyway 持久化岗位、快照、候选、决策与可恢复搜索状态；通过 `roleos-web` 提供 REST 与服务端渲染 Job Pool。实现顺序坚持 Port → Fake Adapter → Contract Test → Real Adapter，并用结构化 Agent 输出补充语义信号，绝不让 Agent 改写 Workflow 或用户策略。

## 技术上下文

**语言/版本**：Java 21
**主要依赖**：Spring Boot 4.0.8、Spring MVC/Security/Validation、Spring Data JPA/JDBC、Flyway、MCP Java SDK BOM 2.0.0（`mcp-core` + Jackson 2 绑定）、Thymeleaf、Micrometer
**存储**：PostgreSQL；Flyway 从 V7 起增加 Job Intelligence 表
**测试**：JUnit 5、Spring Boot Test、MockMvc、Testcontainers PostgreSQL、现有模块边界断言
**目标平台**：本地 macOS/Linux 服务端；用户可见 Chrome/Edge 会话；Playwright MCP 需要 Node.js 20+
**项目类型**：Maven 多模块模块化单体 + 服务端 Web UI + REST API
**性能目标**：Job Pool 本地查询 95% 在 2 秒内完成；用户策略决策在 2 秒内一致可见；单次低频搜索在 5 分钟内产生首批候选
**约束**：单用户优先；不绕过验证码/风控；浏览 Provider 不迁移 Session/ElementRef；Recruiter Activity 只能是弱信号；Reject 不进入 Ranking；Unknown 不得当作 Match；真实外部 Demo 依赖用户已有登录会话
**规模/范围**：V1 单次低频搜索与数十到数百条 Job Pool；一个真实来源 Boss；一个 Job Pool 页面；不建设批量爬虫、微服务、消息队列或复杂 ML Ranking

## 章程检查

*门禁：Phase 0 前通过；Phase 1 设计后复核。*

| 章程约束 | 计划响应 | 结果 |
|---|---|---|
| 真实性与 Provenance | 保存 Boss 原始快照、来源 URL、采集时间和内容摘要；缺失字段一律 Unknown | PASS |
| Upgrade Before Rewrite | 002 不生成 Claim、不改写经历、不触发项目升级 | PASS |
| 模块化单体与既定技术基线 | 复用现有 Maven 模块，不新增服务或基础设施 | PASS |
| Workflow Outside, Agent Inside | 搜索恢复、暂停和策略决策由 Java 状态机与持久化规则控制；Agent 只给结构化 Ranking 信号 | PASS |
| Agent for Judgment, Code for Rules | 城市、薪资、经验、目标 Role、去重和最终加权均为确定性代码 | PASS |
| Port / Adapter 隔离 | `roleos-job` 定义端口并通过通用 `AgentRuntimePort` 调用判断；`roleos-browser`、`roleos-storage` 提供外部适配 | PASS |
| Browser 三层隔离 | 固定 `Job Intelligence → JobSiteAdapter → BrowserRouter → BrowserProvider` | PASS |
| Provider Session 不迁移 | 切换后只读取 `JobSearchState` 并重新 navigate/snapshot | PASS |
| Human Approval | Targeted 推荐不改策略；Captcha/风险提示进入 `PAUSED_FOR_HUMAN` | PASS |
| 外部副作用 | 002 只读取岗位并打开来源链接，不实现投递、消息或撤回 | PASS |
| 可验证交付 | Unit、Repository/Testcontainers、Agent Contract、Adapter Contract、Recovery、MockMvc、Golden Scenario、手工真实 Demo 分层验收 | PASS |
| V1 范围纪律 | 只实现一个来源、轻量分析和用户决策，不提前实现 003～007 | PASS |

**Phase 1 复核**：数据模型把 Job 事实与 Candidate/Decision 分离；接口不暴露浏览句柄；合同明确人工暂停、幂等与 Unknown 语义；无章程偏离，仍为 PASS。

## 项目结构

### 本 Feature 文档

```text
specs/002-job-intelligence/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── job-intelligence-api.yaml
│   ├── ranking-agent.schema.json
│   └── browser-provider-contract.md
├── checklists/requirements.md
└── tasks.md
```

### 源码布局

```text
roleos-job/
├── src/main/java/io/roleos/job/
│   ├── domain/              # Job、Candidate、Decision、Search、Filter、Ranking 值对象
│   ├── port/                # Repository、JobSource、SemanticRanking 端口
│   ├── service/             # Normalize、Dedup、Filter、Ranking、策略与搜索编排
│   └── config/              # 模块装配与可配置权重
└── src/test/java/io/roleos/job/

roleos-browser/
├── src/main/java/io/roleos/browser/
│   ├── port/                # BrowserProvider 契约与 Provider 私有引用
│   ├── router/              # 主备路由及恢复分类
│   ├── playwright/          # MCP 客户端适配
│   ├── kimi/                # 本地 Agent/Kimi Bridge 命令适配
│   └── boss/                # Boss JobSiteAdapter 与快照解析
└── src/test/java/io/roleos/browser/

roleos-storage/
├── src/main/java/io/roleos/storage/job/       # Job 端口 PostgreSQL 适配
├── src/main/resources/db/migration/V7__job_intelligence.sql
└── src/test/java/io/roleos/storage/job/

roleos-web/
├── src/main/java/io/roleos/web/api/job/       # Search/Pool/Detail/Decision REST
├── src/main/java/io/roleos/web/ui/job/        # Job Pool 页面
├── src/main/resources/templates/jobs/         # list/detail 模板
└── src/test/java/io/roleos/web/job/

roleos-boot/
└── src/test/java/io/roleos/boot/JobIntelligenceGoldenScenarioIT.java
```

**结构决策**：不创建新 Maven 模块。`roleos-job` 是领域与用例核心，只依赖稳定的领域基础和端口；`roleos-browser` 依赖 `roleos-job` 并实现真实招聘站点入口；`roleos-storage`、`roleos-web` 依赖 `roleos-job` 提供持久化和交互适配；Boot 只负责装配。浏览 SDK、Boss 页面结构、选择器或 Provider 句柄不得进入 `roleos-job`/`roleos-domain`。

## 分阶段实施

### Phase 1：领域模型与持久化地基

- 定义 Job、SourceSnapshot、JobCandidate、JobDecision、JobSearch、FilterEvaluation、RankingEvaluation 及枚举和值对象。
- 定义 Repository/JobSource/SemanticRanking 端口与幂等、唯一性契约。
- 增加 V7 迁移、PostgreSQL 适配和 Repository/Testcontainers 测试。

### Phase 2：确定性岗位管道

- 实现标准化、双层去重、快照更新、City/Salary/Experience/TargetRole 规则。
- 实现 Rule Signals、弱 Recruiter Activity 权重、结构化 Agent 结果校验和降级。
- 用 20 条 Fixture 验证 Pass/Reject/Unknown、重复和 Ranking 可解释性。

### Phase 3：浏览边界与 Boss 适配

- 先实现 FakeBrowserProvider、FakeJobSource 和契约测试。
- 实现 BrowserRouter、错误分类、业务状态恢复、人工验证暂停。
- 使用 MCP Java SDK 接入 Playwright MCP；通过可配置本地命令契约接入 Kimi Browser Extension，不假定其未公开内部协议。
- 实现 Boss 列表/详情快照解析和脱敏日志；保存实际 Provider、失败原因与采集证据。

### Phase 4：应用接口与 Job Pool UI

- 实现搜索、状态/恢复、列表、详情、Promote/Keep/Skip REST 契约。
- 实现服务端渲染 Job Pool，覆盖空态、加载/失败、Unknown、人工暂停和来源跳转。
- 接入认证用户解析、统一错误码、Trace ID、结构化日志和指标。

### Phase 5：验证与真实 Demo

- 运行模块测试、全量 Maven 测试、格式与静态质量门禁、数据库迁移验证。
- 运行 Fake Provider Golden Scenario 与 Playwright→Kimi 恢复场景。
- 在用户可见且已登录会话中执行一次真实 Boss 低频搜索；若出现验证码则按设计暂停并保留证据。
- 对照 Spec/Plan/Tasks/Constitution 收敛遗漏并生成验收报告。

## 复杂度跟踪

无需要章程例外的设计。MCP Java SDK 与 Thymeleaf 是计划明确列出的两个新增第三方依赖，分别用于既定 Playwright MCP 集成和最小生产级 Web UI；不引入前端构建链或自研 Browser Runtime。
