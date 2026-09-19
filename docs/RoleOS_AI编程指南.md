# RoleOS AI 编程指南

> 本文档定义 RoleOS 的 AI 编程实施方法、编码约束和构建顺序。  
> 前置阅读：《AI Career Agent：深度调研与技术架构报告》《RoleOS 需求文档》《RoleOS 技术方案》。  
> 本文参考 OryxOS 示例项目的核心思想：**Spec-Driven Development、按 User Story 纵向拆解、每个 Story 可独立验收、AI Agent 不自行修改架构约束、主体开发与小增量采用不同协作模式**。
>
> **RoleOS 的推荐协作模式**：
>
> ```text
> 主体开发：Spec-Kit + Codex
> 增量开发：手动任务说明 + Codex
> ```
>
> 本文讲“怎么让 AI 正确地把 RoleOS 做出来”，不替代需求文档和技术方案，也不把代码实现细节提前锁死。

---

# 1. 实施总览

## 1.1 核心思想：Spec 驱动，而不是 Prompt 驱动

RoleOS 不采用：

```text
想到一个功能
→ 给 Codex 一段 Prompt
→ 直接修改代码
→ 再看能不能跑
```

而采用：

```text
Research
   ↓
Requirement
   ↓
Technical Solution
   ↓
Constitution
   ↓
Spec
   ↓
Plan
   ↓
Tasks
   ↓
Codex Implementation
   ↓
Test / Analyze
   ↓
Demo
```

核心目标是：

> **让文档成为 AI 编码行为的约束，而不是让 AI 在编码阶段重新设计产品和架构。**

---

## 1.2 主体开发与增量开发采用不同模式

RoleOS 的开发分两个阶段。

| 阶段 | 推荐方式 | 适用场景 |
|---|---|---|
| **V1 主体开发** | Spec-Kit + Codex | 14 个 Maven 模块、跨模块 Workflow、Browser、Codex、Evidence 等完整闭环 |
| **V1 后增量开发** | 手动任务说明 + Codex | Bug、单个 Adapter、小功能、Prompt 调整、一个 API、一个字段 |

### 主体阶段为什么使用 Spec-Kit

RoleOS V1 具备：

- 明确需求文档；
- 明确技术方案；
- 多模块工程；
- 多个外部 Adapter；
- 持久化 Workflow；
- 较强领域约束；
- 明确 Golden Scenario。

这类项目如果只使用临时 Prompt，容易出现：

```text
需求漂移
架构漂移
模块边界漂移
AI 自行简化设计
测试滞后
```

Spec-Kit 的主要价值不是“帮忙写更多代码”，而是：

> **把已有需求和技术方案转换成 AI 可以持续遵守的工程契约。**

---

## 1.3 四份文档的职责必须分开

```text
调研报告
→ 为什么这么选

需求文档
→ 做什么

技术方案
→ 怎么设计

AI 编程指南
→ AI 按什么顺序和纪律把它实现出来
```

禁止让 Codex 在实现阶段重新回答前面三个问题。

例如技术方案已经明确：

```text
OryxOS = Agent Runtime
Career Workflow = RoleOS 自研
Playwright / Kimi = Browser Adapter
Codex = Coding Agent
Career DB = Structured Facts
```

调用层再细分为：

```text
AgentRuntimePort → OryxOS → 模型 Provider / OpenAI API
CodingAgentPort  → Codex  → 代码执行环境 / 模型调用
OpenAPI          → RoleOS 对外 REST 接口文档
```

即使 OryxOS 与 Codex 最终都使用同一模型提供商，也必须保留两个 Port 和独立的审计、
成本及权限边界。不得为了减少 Adapter 数量，把 Codex 工程执行能力放入对话 Agent，
或把 OryxOS Agent 作为项目代码修改器。

Codex 不应在编码时自行改成：

```text
全流程 LangGraph
直接使用 Playwright Page
全部状态放 Redis
让 OryxOS Memory 保存 Career Fact
```

除非先修改技术方案并进行人工决策。

---

# 2. RoleOS 的 AI 编码原则

以下原则属于整个主体开发阶段的非协商规则。

---

## 2.1 Golden Scenario First

所有代码都应该服务于：

```text
真实 Job
→ JD Analysis
→ Experience Match
→ Gap Analysis
→ Experience Mining
→ Project Upgrade
→ Evidence
→ Resume
→ Human Approval
→ Application
→ Outcome
```

优先级：

```text
能跑通真实闭环
>
模块数量
>
架构完整度
>
高级扩展能力
```

V1 不允许因为以下能力拖慢 Golden Scenario：

- Multi-Agent；
- Career Knowledge Graph；
- 多招聘网站；
- 高级向量检索；
- 自研 Browser Runtime；
- 自研 Coding Agent；
- 微服务拆分。

---

## 2.2 Workflow Outside, Agent Inside

宏观流程：

```text
Code / Workflow
```

复杂判断：

```text
Agent
```

例如：

| 任务 | 实现方式 |
|---|---|
| 城市过滤 | Java Rule |
| 薪资过滤 | Java Rule |
| Job 去重 | Java |
| Workflow Transition | Java |
| Application 幂等 | Java / DB |
| JD Requirement 理解 | Agent |
| Experience Match | Agent + Structured Facts |
| Experience Mining 问题生成 | Agent |
| Upgrade Plan | Agent |
| Resume Narrative | Agent |

最容易犯的错误：

```text
让 Agent 决定 Workflow currentStage
```

禁止。

Agent 只能返回：

```text
Decision
Recommendation
Structured Result
```

最终状态转换由 `roleos-workflow` 完成。

---

## 2.3 Agent for Judgment, Code for Rules

如果一件事情可以稳定地由代码完成，就不要让 LLM 每次重新推理。

例如：

```text
salaryMin >= expectedSalary
```

不应该调用 Agent。

目的：

- 降低 Token；
- 提高稳定性；
- 提高可测试性；
- 减少 Prompt 复杂度。

---

## 2.4 Database 管事实，Memory 管上下文

Career DB 保存：

```text
Experience
Project
Skill
Capability
Job
Requirement
Gap
Claim
Evidence
Workflow
Resume
Application
Outcome
```

OryxOS Memory 保存：

```text
Preference
Conversation Context
Agent Interaction Experience
Semantic Context
```

禁止：

> 仅因为 Agent 在一次对话中推断“用户熟悉 MCP”，就直接把 `MCP = 熟练` 写进 Career Profile。

---

## 2.5 Never Fabricate Experience

必须保持：

```text
User Fact
≠
Agent Inference
≠
Designed Scenario
≠
Upgraded Capability
```

Claim 必须具有 Provenance。

任何：

```text
虚构用户数
虚构客户
虚构收入
虚构生产规模
虚构性能指标
虚构历史项目
```

都属于硬失败。

---

## 2.6 Upgrade Before Rewrite

发现真实 Skill Gap：

```text
Skill Gap
→ Project Upgrade
→ Test / Eval
→ Evidence
→ Claim
```

而不是：

```text
Skill Gap
→ LLM 改写
→ Resume
```

---

## 2.7 External Capability Must Be Replaceable

以下能力都必须通过 Port / Adapter 接入：

```text
OryxOS
Browser
Coding Agent
Evaluation
Resume Renderer
Job Source
```

Career Domain 不允许直接依赖具体实现。

---

## 2.8 Human in Control

以下节点是强制 Human Gate：

```text
Experience Confirmation
Project Upgrade Approval
Resume Approval
Application Approval
Captcha / Human Verification
Unknown External Side Effect
```

Codex 不允许“为了跑通 Demo”删除这些 Approval。

---

# 3. Spec-Kit 准备阶段

正式编码前，先完成 Spec-Kit artifacts。

---

## 3.1 输入文档

仓库建议保存：

```text
docs/
├── ResearchReport.md
├── DemandAnalysis.md
├── TechnicalSolution.md
└── AiProgrammingGuide.md
```

分别对应：

```text
ResearchReport
→ 调研结论

DemandAnalysis
→ RoleOS 需求文档

TechnicalSolution
→ RoleOS 技术方案

AiProgrammingGuide
→ 本文
```

AI 开发时必须使用最新版文档。

---

## 3.2 Constitution：把最容易写错的东西写死

`constitution.md` 是主体阶段最高优先级工程约束。

建议至少写入以下原则。

### Principle 1 — Java 技术基线

```text
JDK 21
Spring Boot 3.x
Maven Multi-Module
PostgreSQL
Flyway
```

主体阶段不随意替换。

---

### Principle 2 — Career Domain 独立于 OryxOS

只有：

```text
roleos-runtime-oryx
```

允许直接依赖 OryxOS Runtime API。

禁止：

```text
roleos-job
roleos-experience
roleos-resume
roleos-domain
```

直接 import OryxOS 内部类。

核心原则：

> **借 Runtime，不借业务。**

---

### Principle 3 — Durable Workflow

Career Workflow 必须持久化。

不能仅存在于：

```text
Java Memory
LLM Session
OryxOS Memory
Chat History
```

服务重启后 Workflow 必须能够继续。

---

### Principle 4 — Agent 不能修改 Workflow State

禁止：

```text
LLM response:
{
  "currentStage": "APPLICATION_SUBMIT"
}
```

然后直接写数据库。

必须：

```text
Agent Decision
→ Workflow Rule
→ Transition
```

---

### Principle 5 — Career Fact 必须有 Provenance

所有关键 Career Fact 必须区分：

```text
USER_CONFIRMED
SOURCE_CODE
PROJECT_UPGRADE
AGENT_INFERENCE
DESIGNED_SCENARIO
```

`AGENT_INFERENCE` 不得自动变成真实事实。

---

### Principle 6 — Claim 必须经过 Evidence Policy

Resume Compiler 默认只能使用：

```text
VERIFIED
SUPPORTED
```

禁止使用：

```text
UNSUPPORTED
```

---

### Principle 7 — Codex Output ≠ Upgrade Completed

Codex 返回：

```text
completed
```

不代表 Project Upgrade 完成。

必须经过：

```text
Build
Test
Evaluation
Artifact Check
```

再由 RoleOS 判定：

```text
PASS
PARTIAL
FAIL
```

---

### Principle 8 — Browser 三层隔离

固定：

```text
Career Domain
→ JobSiteAdapter
→ BrowserRouter
→ BrowserProvider
```

禁止 Domain 直接出现：

```text
Playwright Page
CSS Selector
XPath
Kimi WebBridge Handle
```

---

### Principle 9 — Browser Session 不跨 Provider 迁移

Playwright MCP 与 Kimi WebBridge 可以切换。

但：

```text
ElementRef
DOM Snapshot
Tab Handle
Browser Session
```

不能跨 Provider 复用。

切换后通过：

```text
BrowserTaskState
→ Navigate
→ Snapshot
→ Resume Business Step
```

恢复。

---

### Principle 10 — Side Effect 不能盲目 Retry

以下行为：

```text
SUBMIT_APPLICATION
SEND_MESSAGE
WITHDRAW_APPLICATION
```

失败后如果无法确认是否已经产生外部副作用：

```text
→ PAUSED_FOR_HUMAN
```

禁止切 Provider 后直接再执行一次。

---

### Principle 11 — Broad 与 Targeted 成本必须分离

禁止把：

```text
Experience Mining
Project Upgrade
Job-specific Resume
```

默认运行在所有 Broad Job 上。

---

### Principle 12 — V1 不提前建设高级架构

V1 禁止 AI 自行引入：

- Kafka；
- Kubernetes 微服务；
- Graph DB；
- Redis Workflow Engine；
- Temporal；
- LangGraph 作为总 Workflow；
- 多 Agent Delegation Framework；

除非技术方案先明确变更。

---

## 3.3 Constitution 不能由 AI 自行修改

如果 Codex 发现：

```text
当前实现和 constitution 冲突
```

正确行为是：

```text
停止
→ 报告冲突
→ 用户做架构决策
```

不是：

```text
修改 constitution
→ 让实现合法
```

---

# 4. AGENTS.md：Codex 的仓库级执行规则

除 Spec-Kit Constitution 外，建议在仓库根目录维护：

```text
AGENTS.md
```

作用：

> **把高频编码纪律转成 Codex 每次进入仓库都能直接看到的规则。**

建议包含：

```markdown
# RoleOS Coding Rules

1. Read docs/DemandAnalysis.md and docs/TechnicalSolution.md before architectural changes.
2. Domain modules must not depend on OryxOS / Playwright / Kimi / Codex implementation classes.
3. Workflow state transitions only happen through roleos-workflow.
4. Agent output must be schema validated before persistence.
5. Never persist Agent inference as user fact without confirmation/evidence.
6. Codex upgrade completion requires independent validation.
7. Browser provider switching never migrates ElementRef/session state.
8. Side-effect operations require idempotency and approval.
9. Every task must add/update tests.
10. Do not introduce new infrastructure without explicit plan change.
```

如果某个模块需要更具体规则，可以增加模块级 `AGENTS.md`，但不要大量复制。

---

# 5. `/speckit.specify`：按 7 个 User Story 拆 RoleOS

RoleOS 不按照：

```text
14 个 Maven 模块
```

拆 Story。

模块是代码结构。

User Story 应按照：

> **一个可以独立验证的业务纵向切片**

拆分。

推荐 7 个 User Story。

---

## US-1：Career Foundation + Durable Workflow

目标：

> 建立后续所有业务能力依赖的 Career Domain、Career DB 和持久化 Workflow。

涉及：

```text
roleos-domain
roleos-application
roleos-workflow
roleos-storage
roleos-runtime-oryx
roleos-web
roleos-boot
```

主要能力：

- CareerProfile；
- Role；
- Experience；
- Project；
- WorkflowInstance；
- ApprovalTask；
- PostgreSQL；
- Flyway；
- OryxOS Adapter；
- Structured Agent Invocation。

Demo：

```text
创建 Career Profile
→ 创建 Workflow
→ 调用 Career Agent
→ WAITING_APPROVAL
→ 重启服务
→ Resume Workflow
```

US-1 的关键不是 UI，而是：

> **Durable Workflow + Domain Boundary 成立。**

### 当前实现状态（2026-09-18）

Career Foundation 已按上述边界交付：职业档案与资产、技能多来源及可信度视图、PostgreSQL/Flyway
持久化 Workflow、等待审批后的重启恢复、重复 `commandId` 幂等决定，以及统一的认证、错误、Trace ID
与脱敏响应契约。

导入能力当前仅是受控的 Fake Adapter / `fixture://` 测试入口：候选项必须由用户确认、编辑或拒绝，只有
确认后的内容才会写入 Career Asset；它不是生产简历解析器，也不能把原始简历或未经确认候选项变成用户事实。

验证入口见 [`specs/001-career-foundation/quickstart.md`](../specs/001-career-foundation/quickstart.md)。核心命令为：

```bash
mvn -B -ntp -pl roleos-web -am test -Dtest=CrossCuttingApiContractTest -Dsurefire.failIfNoSpecifiedTests=false
mvn -B -ntp -pl roleos-boot -am test -Dtest=CareerFoundationGoldenScenarioIT -Dsurefire.failIfNoSpecifiedTests=false
mvn -B -ntp -Pquality verify
```

---

## US-2：Job Intelligence + Browser

目标：

> 从一个真实招聘来源获取岗位，并完成 Broad / Targeted 处理入口。

涉及：

```text
roleos-job
roleos-browser
roleos-workflow
roleos-storage
roleos-runtime-oryx
```

实现：

```text
JobSource
JobSiteAdapter
BrowserRouter
BrowserProvider
PlaywrightMcpBrowserProvider
KimiWebBridgeBrowserProvider
Normalize
Dedup
Hard Filter
Fast JD Analysis
Broad Ranking
Targeted Promotion
```

Demo：

```text
真实招聘页面
→ 获取 Job
→ Normalize
→ Filter
→ Ranking
→ 输出 Top Jobs
```

同时至少验证一次：

```text
Playwright Failure
→ BrowserRouter
→ Kimi WebBridge
→ 从 Business State 恢复
```

---

## US-3：JD & Experience Match

目标：

> 把 Job Requirement 与用户真实 Experience 建立结构化匹配。

涉及：

```text
roleos-experience
roleos-job
roleos-domain
roleos-runtime-oryx
roleos-storage
```

实现：

```text
Requirement Extraction
Experience Retrieval
Experience Match
Story Gap
Skill Gap
Evidence Gap
Upgrade Feasibility
```

Demo：

```text
真实 JD
+
真实 Career Profile
→ 找到最相关 Project
→ 输出 Match
→ 输出三类 Gap
```

---

## US-4：Experience Builder + HITL

目标：

> 通过有限的用户追问，把“信息不足”转化为真实且完整的 Experience Asset。

涉及：

```text
roleos-experience
roleos-workflow
roleos-domain
roleos-storage
roleos-runtime-oryx
```

实现：

```text
Question Planner
Mining Stop Condition
Fact Provenance
Experience Confirmation
Story Enhancement
Upgrade Candidate
```

Demo：

```text
一段简略 Experience
→ Agent 提问
→ User 回答
→ Experience Confirmation
→ Experience Asset
```

---

## US-5：Project Upgrade + Codex + Evidence

目标：

> 将一个真实 Skill Gap 转化为真实工程能力。

涉及：

```text
roleos-upgrade
roleos-codex
roleos-evidence
roleos-workflow
roleos-storage
```

实现：

```text
Upgrade Plan
Human Approval
CodingAgentPort
CodexAdapter
Git Worktree
Upgrade Task Contract
Validation Runner
Evidence Collector
```

Demo：

```text
Skill Gap
→ Upgrade Plan
→ User Approval
→ Codex 修改真实 Repo
→ Test / Eval
→ Evidence
```

这是 V1 最关键的技术 Story。

---

## US-6：Claim-Evidence + Resume + Interview

目标：

> 将验证后的真实能力转化为可信的求职表达。

涉及：

```text
roleos-evidence
roleos-resume
roleos-experience
roleos-storage
roleos-runtime-oryx
```

实现：

```text
Claim Level
Claim Policy
Evidence Mapping
Role Resume
Job-specific Resume
Interview Story
Resume Version
Human Review
```

Demo：

```text
Resume Claim
→ Evidence
→ Interview Story
```

三者互相可追溯。

---

## US-7：Application + Feedback

目标：

> 完成正式投递与 Outcome Feedback 闭环。

涉及：

```text
roleos-application-tracking
roleos-browser
roleos-workflow
roleos-storage
roleos-web
```

实现：

```text
Application Preparation
Application Approval
Submit Idempotency
Browser Side Effect Guard
Outcome
Career Feedback
Broad / Targeted Metrics
```

Demo：

```text
Approved Resume
→ Human Approval
→ Application
→ Outcome
→ Career Feedback
```

---

# 6. User Story 依赖关系

推荐推进：

```text
US-1
  ↓
US-2
  ↓
US-3
  ↓
US-4
  ↓
US-5
  ↓
US-6
  ↓
US-7
```

RoleOS 的 7 个 Story 比示例项目更偏串行。

原因：

```text
Job
是 Match 的输入

Match
是 Experience Builder 的输入

Gap
是 Project Upgrade 的输入

Evidence
是 Resume 的输入

Resume
是 Application 的输入
```

部分工程任务可以并行，但 **业务 Story 不建议跳跃开发**。

例如 US-2 还没有稳定 Job Schema 时，不建议提前正式实现 US-3 Match。

---

# 7. `/speckit.plan`：从技术方案生成实施 Plan

Plan 必须严格对齐最新技术方案。

重点检查：

```text
Java 21
Spring Boot 3.x
PostgreSQL + Flyway
14 Maven Modules
Career Workflow 自研
OryxOS Adapter
BrowserRouter
Playwright MCP
Kimi WebBridge
CodingAgentPort
CodexAdapter
Claim-Evidence
```

---

## 7.1 Plan Review Checklist

Spec-Kit 生成 Plan 后，人工检查：

- [ ] 是否把 RoleOS 设计成模块化单体；
- [ ] 是否保持 14 个技术方案模块职责；
- [ ] 是否只有 `roleos-runtime-oryx` 直接依赖 OryxOS；
- [ ] 是否把 Career Workflow 持久化；
- [ ] 是否避免把 Agent Session 当 Workflow；
- [ ] 是否存在 BrowserRouter；
- [ ] 是否支持 Playwright / Kimi 可切换；
- [ ] 是否明确 Browser Session 不迁移；
- [ ] 是否有 CodingAgentPort；
- [ ] 是否有 Codex 独立 Validation；
- [ ] 是否有 Claim-Evidence Provenance；
- [ ] 是否有 Human Approval；
- [ ] 是否有 Application Idempotency；
- [ ] 是否没有提前引入微服务 / MQ / Graph DB。

任何一项不满足：

> Plan 不进入 implement。

---

# 8. `/speckit.tasks`：Task 如何拆才适合 Codex

## 8.1 不要生成超大 Task

错误：

```text
T-001 实现 RoleOS Job Intelligence
```

这种任务太大。

正确：

```text
T-001 Define Job canonical domain model
T-002 Add Flyway migration for jobs
T-003 Implement JobRepository
T-004 Define JobSource port
T-005 Implement FakeJobSource
T-006 Implement JobNormalizationService
T-007 Implement JobFilterService
T-008 Add unit tests
T-009 Implement Playwright adapter
T-010 Add Browser contract tests
```

原则：

> **一个 Task 应该能被 Codex 在有限上下文中独立理解、修改和验证。**

---

## 8.2 Task 推荐结构

每个 Task 至少写：

```text
Goal
Files / Modules
Inputs
Expected Behavior
Constraints
Tests
Done Criteria
```

例如：

```text
Task:
Implement Workflow transition for PROJECT_UPGRADE_APPROVAL

Module:
roleos-workflow

Behavior:
APPROVED → PROJECT_UPGRADE
REJECTED → EVIDENCE_BUILD
DEFERRED → WAITING_APPROVAL

Constraints:
- Agent cannot set stage directly
- Persist transition event
- Optimistic lock required

Tests:
- approved transition
- rejected transition
- concurrent update conflict
```

---

## 8.3 先 Port，再 Fake，再 Real Adapter

所有外部系统统一按：

```text
Port
→ Fake Adapter
→ Contract Test
→ Real Adapter
```

例如 Browser：

```text
BrowserProvider
→ FakeBrowserProvider
→ BrowserProviderContractTest
→ PlaywrightMcpBrowserProvider
→ KimiWebBridgeBrowserProvider
```

Coding Agent：

```text
CodingAgentPort
→ FakeCodingAgent
→ Contract Test
→ CodexAdapter
```

这样核心 Domain 不会因为外部工具暂时不可用而被阻塞。

---

# 9. 每个 User Story 的标准编码循环

每个 Story 固定使用同一开发循环。

---

## Step 1：让 Codex 重读上下文

至少读取：

```text
constitution.md
spec.md
plan.md
当前 Story tasks.md
相关技术方案章节
相关现有代码
```

不要假设 Codex 记得上一轮上下文。

---

## Step 2：先检查现有实现

要求 Codex：

```text
先读代码
→ 列出已有能力
→ 列出本 Task 需要修改的文件
→ 再编码
```

禁止：

```text
没读现有实现
→ 新建一套重复 Service
```

---

## Step 3：先写测试或验收条件

对于确定性逻辑：

```text
Test First
```

例如：

- Workflow Transition；
- Ranking Formula；
- Claim Policy；
- Idempotency；
- Browser Router。

对于 Agent 逻辑：

```text
Fixture + Structured Output Contract
```

---

## Step 4：实现最小代码

Codex 应优先修改最少文件。

如果一个 Task 突然需要修改：

```text
10+ unrelated files
```

应停下来检查：

- Task 是否太大；
- 模块边界是否错；
- 是否发生架构漂移。

---

## Step 5：自动验证

至少执行：

```text
mvn test
```

按 Story 需要额外运行：

```text
Integration Test
Contract Test
Testcontainers
Agent Fixture Test
Browser Test
Golden Scenario Partial Demo
```

---

## Step 6：检查 Architecture Drift

每个 Story 完成后检查：

```text
Constitution
vs
Spec
vs
Plan
vs
Code
```

使用 Spec-Kit analyze 能力时，应将其作为硬性步骤。

---

## Step 7：人工 Review

重点不是看所有代码细节，而是检查：

```text
边界是否被破坏
是否引入额外架构
是否跳过 Approval
是否跳过 Validation
是否把 Agent 推理写成事实
```

---

## Step 8：稳定点 Commit

至少每个 Story 一个稳定 Commit。

推荐：

```text
feat(us-01): durable career workflow foundation
feat(us-02): job intelligence and browser routing
feat(us-03): requirement experience matching
feat(us-04): experience mining and hitl
feat(us-05): codex project upgrade
feat(us-06): claim evidence and resume compiler
feat(us-07): application feedback loop
```

Story 很大时可以拆多个小 Commit，但 Story 完成时必须有一个稳定点。

---

# 10. US-1 编码拆解

## 10.1 目标

建立：

```text
Domain
+
Persistence
+
Workflow
+
Oryx Runtime Boundary
```

---

## 10.2 推荐 Task 顺序

### Foundation

```text
T01 Maven module skeleton
T02 roleos-boot startup
T03 PostgreSQL/Testcontainers
T04 Flyway baseline
```

### Domain

```text
T05 CareerProfile
T06 Role
T07 Experience
T08 Project
T09 Domain IDs / Value Objects
```

### Workflow

```text
T10 WorkflowType / Stage / Status
T11 WorkflowInstance
T12 WorkflowRepository
T13 WorkflowTransitionService
T14 WorkflowEvent
T15 ApprovalTask
T16 Retry / Resume
T17 Optimistic Lock
```

### Oryx Adapter

```text
T18 AgentRuntimePort
T19 FakeAgentRuntime
T20 OryxRuntimeAdapter
T21 Structured Output Validation
```

### API

```text
T22 Career Profile API
T23 Workflow API
T24 Approval API
```

---

## 10.3 最容易写错的地方

### 错误 1

把 Workflow 放进 Agent Session。

正确：

```text
WorkflowInstance
→ PostgreSQL
```

### 错误 2

让 Agent 返回下一个 Stage。

正确：

```text
Agent Decision
→ Transition Rule
```

### 错误 3

所有业务模块依赖 OryxOS。

正确：

```text
Domain
→ AgentRuntimePort
← OryxRuntimeAdapter
```

---

# 11. US-2 编码拆解：Job + Browser

## 11.1 推荐顺序

```text
Job Domain
→ JobSource Port
→ Fake Job Source
→ Normalize
→ Hard Filter
→ Ranking
→ BrowserProvider
→ Fake Browser
→ Contract Test
→ BrowserRouter
→ JobSiteAdapter
→ Playwright
→ Kimi
```

不要一开始就从真实招聘网站 DOM 开始写。

---

## 11.2 Browser Contract Test 优先

Contract 固定：

```text
open
navigate
snapshot
click
fill
upload
health
capabilities
```

Playwright 与 Kimi 都必须通过同一套 Contract Test。

---

## 11.3 Browser Failover Test

必须自动测试：

```text
Playwright Healthy
→ Playwright

Playwright Recoverable Failure
→ Kimi

Manual Kimi
→ Kimi

Captcha
→ PAUSED_FOR_HUMAN

Unknown Side Effect
→ PAUSED_FOR_HUMAN
```

---

## 11.4 禁止跨 Provider ElementRef

Codex 如果生成：

```java
ElementRef ref = playwright.snapshot().find(...);
kimi.click(ref);
```

属于架构错误。

必须重写。

---

# 12. US-3 编码拆解：Match & Gap

推荐：

```text
Requirement Entity
→ Requirement Extraction Schema
→ Fixture-based Agent Test
→ Experience Retrieval
→ Match Schema
→ Match Persistence
→ Gap Rule
→ Upgrade Feasibility
```

重点：

> `sourceText` 必须保留。

每个 Requirement 应该能够追溯：

```text
Requirement
→ JD 原始内容
```

避免 LLM 凭空扩展 Requirement。

---

# 13. US-4 编码拆解：Experience Builder

## 13.1 Question Planner 先结构化

不要先做“智能聊天 UI”。

先实现：

```text
MissingInformation
→ QuestionPlan
→ UserAnswer
→ FactCandidate
→ Confirmation
→ Fact
```

---

## 13.2 Mining Stop Condition 写成代码规则

不能只 Prompt：

> “信息够了就停止。”

至少要有：

```text
criticalRequirementsResolved
projectResponsibilityKnown
storyMinimumFieldsComplete
remainingUnknownDoesNotChangeDecision
```

Agent 提建议，代码决定是否允许结束 Mining。

---

## 13.3 Fact 写库前的 Gate

```text
User Answer
→ Fact Candidate
→ Provenance
→ Confirmation
→ Career DB
```

Agent inference 默认不能直接持久化为事实。

---

# 14. US-5 编码拆解：Codex Project Upgrade

## 14.1 先实现 FakeCodingAgent

在真的调用 Codex 前先跑通：

```text
Upgrade Plan
→ Approval
→ Fake Coding Execution
→ Fake Validation
→ Evidence
```

证明 Workflow 没问题。

---

## 14.2 再实现 CodexAdapter

输入必须是：

```text
UpgradeTaskContract
```

而不是自由文本。

Contract 至少包含：

```text
projectPath
goal
targetCapabilities
constraints
requiredOutputs
validationCommands
```

---

## 14.3 Git Worktree 是强制保护层

推荐：

```text
main
↓
roleos/upgrade/{upgradeId}
↓
Codex
↓
Validation
↓
Review
```

Codex 默认不得直接修改主分支。

---

## 14.4 RoleOS 独立验证

禁止直接相信 Codex：

```json
{
  "status": "completed"
}
```

RoleOS 自己执行：

```text
Build
Test
Eval
Artifact Check
```

---

# 15. US-6 编码拆解：Claim-Evidence

实现顺序：

```text
Evidence
→ Claim
→ ClaimEvidence Mapping
→ Claim Policy
→ Resume Version
→ Resume Compiler
→ Interview Story
```

不要反过来先做简历生成页面。

---

## 15.1 Claim Policy 要有确定性代码

例如：

```text
L4 → REJECT
L5 → REJECT
UNSUPPORTED → REJECT
Quantitative Claim without Eval → REJECT
L2 with Source Code + Test → VERIFIED
```

这些不能全部交给 Agent 判断。

---

# 16. US-7 编码拆解：Application & Feedback

## 16.1 先记录 Application，再自动化页面

第一版可以：

```text
Prepare Application
→ Human Approval
→ Manual Submit
→ Record Outcome
```

先跑闭环。

然后再接 Browser Submit。

这样招聘网站自动化不会阻塞整个 V1。

---

## 16.2 Submit Idempotency

必须先实现：

```text
IdempotencyKey =
userId
+ jobId
+ resumeVersionId
```

再实现自动 Submit。

---

## 16.3 Feedback 不直接改 Career Fact

正确：

```text
Outcome
→ Observation
→ Feedback
→ Recommendation
```

错误：

```text
Interview Rejected
→ user.skillLevel = LOW
```

---

# 17. 测试策略

RoleOS 不采用“代码写完最后统一补测试”。

测试跟 User Story 同步开发。

---

## 17.1 Unit Test

适合：

```text
Filter
Ranking
Gap Routing
Claim Policy
Workflow Transition
Idempotency
Browser Router
```

---

## 17.2 Repository Test

使用 Testcontainers PostgreSQL。

至少验证：

```text
Flyway
JPA Mapping
Optimistic Lock
Workflow Recovery
Unique Constraint
```

---

## 17.3 Agent Contract Test

不要断言：

```text
LLM 输出必须逐字完全相同
```

断言：

```text
JSON Schema
Required Fields
Enum Range
Fact Safety
Source Trace
```

使用固定 Fixture：

```text
jd-fixtures/
experience-fixtures/
agent-results/
```

---

## 17.4 Adapter Contract Test

Browser：

```text
Fake
Playwright
Kimi
```

跑同一 Contract。

Coding：

```text
Fake
Codex
```

跑同一 Contract。

---

## 17.5 Workflow Recovery Test

至少：

```text
Start
→ WAITING_USER
→ Restart
→ Resume
```

```text
Start
→ WAITING_APPROVAL
→ Restart
→ Approve
→ Continue
```

---

## 17.6 Golden Scenario E2E

V1 发布前至少跑一次：

```text
真实 Job
+
真实 Career Profile
+
真实 Project Repo
```

完成：

```text
Job
→ Match
→ Mining
→ Upgrade
→ Evidence
→ Resume
→ Approval
→ Application
```

---

# 18. AI Agent 最容易写错的 20 个地方

编码 Review 时优先检查以下问题。

| # | AI 常见错误 | RoleOS 正确做法 |
|---|---|---|
| 1 | Career Domain 直接依赖 OryxOS | 只能依赖 Port |
| 2 | OryxOS 承担 Career Workflow | RoleOS 自己维护 Durable Workflow |
| 3 | Workflow State 存 Session | PostgreSQL 持久化 |
| 4 | Agent 自己修改 Stage | Workflow Rule 修改 |
| 5 | 所有规则都交给 LLM | Deterministic Rule 用 Java |
| 6 | Agent inference 写成 User Fact | Provenance + Confirmation |
| 7 | Skill Gap 直接改 Resume | Project Upgrade |
| 8 | Codex completed = Upgrade 完成 | 独立 Validation |
| 9 | Codex 修改 main | Git Worktree / Branch |
| 10 | Resume Claim 无 Evidence | Claim Policy 拒绝 |
| 11 | 虚构性能数字 | 必须由 Eval 产生 |
| 12 | Browser 逻辑泄漏到 Domain | JobSiteAdapter + BrowserProvider |
| 13 | Playwright ElementRef 给 Kimi 用 | 禁止跨 Provider |
| 14 | Fallback 后直接重复 Submit | Side Effect Check |
| 15 | 验证码自动绕过 | PAUSED_FOR_HUMAN |
| 16 | Broad Job 全部 Deep Analysis | Broad 走低成本链路 |
| 17 | 一开始接多个招聘网站 | V1 一个真实 Source |
| 18 | 一开始上 Graph DB / Vector DB | V1 Structured + Full Text |
| 19 | 一开始拆微服务 | 模块化单体 |
| 20 | 没有测试就宣称 Story 完成 | Demo + Test + Analyze 才完成 |

---

# 19. Codex 协作任务模板

日常给 Codex 的任务建议采用固定结构。

```markdown
## Task

实现 JobFilterService 的 salary / city / experience hard filter。

## Source of Truth

- docs/DemandAnalysis.md: Job Intelligence
- docs/TechnicalSolution.md: Chapter 5
- constitution.md

## Scope

Module:
roleos-job

Allowed changes:
- roleos-job
- related tests

Do not change:
- domain architecture
- browser adapter
- workflow stages

## Expected Behavior

1. city mismatch → REJECT
2. salary below minimum → REJECT
3. missing salary → UNKNOWN
4. experience mismatch → REJECT

## Constraints

- deterministic Java logic
- no LLM call
- output reason must be explainable

## Tests

Add unit tests for PASS / REJECT / UNKNOWN.

## Done

mvn test passes.
```

目的：

> **任务 Prompt 描述实施工作，不重新描述整个项目。**

---

# 20. Codex 开始每个 Story 前的固定指令

每个 Story 开始时建议要求 Codex：

```text
1. 阅读 constitution.md
2. 阅读当前 spec / plan / tasks
3. 阅读相关需求文档章节
4. 阅读相关技术方案章节
5. 扫描目标 Maven 模块现有代码
6. 列出当前实现和目标之间的 Gap
7. 不编码，先给出本 Story 的执行顺序
8. 确认没有架构冲突后再开始第一个 Task
```

避免让 Codex：

```text
收到 Story 名
→ 立即生成几十个 Java 文件
```

---

# 21. 一次只做一个 Task

AI Coding 最大的问题之一是一次修改太多。

建议：

```text
Task
→ Code
→ Test
→ Review
→ Commit
→ Next Task
```

而不是：

```text
10 Tasks
→ 一次 implement
→ 3000 行 diff
→ 无法 Review
```

---

# 22. Definition of Done

一个 Task 完成：

- [ ] 行为符合 Task；
- [ ] Test 通过；
- [ ] 没有违反 Constitution；
- [ ] 没有无关重构；
- [ ] 没有 TODO 替代核心逻辑。

一个 User Story 完成：

- [ ] Story 所有核心 Task 完成；
- [ ] Unit / Integration / Contract Test 通过；
- [ ] Spec-Kit Analyze 无重大漂移；
- [ ] 对应 Demo 可运行；
- [ ] 核心日志 / Trace 可查看；
- [ ] 人工 Review 通过；
- [ ] 有稳定 Git Commit。

V1 完成：

> **Golden Scenario 使用真实数据完整跑通。**

---

# 23. Git 与分支策略

建议：

```text
main
│
├── feat/us-01-career-foundation
├── feat/us-02-job-intelligence
├── feat/us-03-experience-match
├── feat/us-04-experience-builder
├── feat/us-05-project-upgrade
├── feat/us-06-claim-resume
└── feat/us-07-application-feedback
```

每个 Story：

```text
branch
→ tasks
→ tests
→ analyze
→ demo
→ PR / merge
```

Project Upgrade 产生的用户项目 Branch：

```text
roleos/upgrade/{upgradeId}
```

和 RoleOS 自己开发分支是两个概念，不要混用。

---

# 24. 主体阶段完成后的增量开发

RoleOS V1 主体闭环完成后，不要求所有小修改继续走完整 Spec-Kit。

---

## 24.1 直接使用 Codex 的情况

适合：

- 一个 Bug；
- 一个 API；
- 一个 JobSiteAdapter；
- 一个 Browser Provider Bug；
- 一个 Ranking Factor；
- 一个 Resume Template；
- 一个小型 DB Migration；
- 一个 Prompt 优化。

流程：

```text
Issue
→ Task Contract
→ Codex
→ Test
→ Review
→ PR
```

---

## 24.2 应重新走 Spec-Kit 的情况

如果变更涉及：

```text
新增 Maven Module
改变 Constitution
改变 Domain Boundary
新增 Workflow Type
引入新基础设施
跨 3+ 核心业务能力
```

则应重新执行：

```text
specify
→ plan
→ tasks
→ implement
```

---

# 25. 项目交付物

主体开发完成后，仓库至少包含：

```text
docs/
├── ResearchReport.md
├── DemandAnalysis.md
├── TechnicalSolution.md
└── AiProgrammingGuide.md

.specify/
├── memory/
│   └── constitution.md
└── specs/
    └── ...

AGENTS.md

roleos-domain/
roleos-application/
roleos-workflow/
roleos-job/
roleos-experience/
roleos-upgrade/
roleos-evidence/
roleos-resume/
roleos-application-tracking/
roleos-browser/
roleos-codex/
roleos-runtime-oryx/
roleos-storage/
roleos-web/
roleos-boot/
```

---

# 26. 实施过程中的风险控制

## 26.1 Spec 与实现漂移

应对：

```text
每个 Story 完成后 Analyze
```

不要等 V1 做完才统一检查。

---

## 26.2 Codex 上下文丢失

每个 Story / 大 Task 开始：

```text
重新读取 Source of Truth
```

不依赖聊天上下文记忆。

---

## 26.3 AI 过度重构

Task 明确：

```text
Allowed Modules
Do Not Change
```

没有必要，不做跨模块重构。

---

## 26.4 AI 过度设计

发现 Codex 引入：

```text
Kafka
Redis
Temporal
Graph DB
Microservices
```

立即检查技术方案。

没有明确需求时撤回。

---

## 26.5 外部系统导致开发阻塞

Browser / Codex / OryxOS 均先使用 Fake Adapter。

核心 Workflow 不能因为某个外部工具暂时不可用就停止开发。

---

# 27. 推荐主体开发顺序

最终建议：

```text
准备阶段
│
├── Constitution
├── AGENTS.md
├── Spec
├── Plan
└── Tasks
     ↓
US-1 Career Foundation
     ↓
US-2 Job Intelligence
     ↓
US-3 JD & Experience Match
     ↓
US-4 Experience Builder
     ↓
US-5 Project Upgrade + Codex
     ↓
US-6 Claim-Evidence + Resume
     ↓
US-7 Application + Feedback
     ↓
Golden Scenario
```

---

# 28. 最终编码原则

RoleOS 的 AI 编码不追求：

> “让 Codex 一次生成整个项目。”

而追求：

> **让 Codex 在明确 Spec、稳定架构边界和小颗粒度 Task 下，持续产生可验证的工程增量。**

整个开发过程中始终保持：

```text
Spec before Code
Domain before Adapter
Port before Implementation
Fake before External Integration
Rule before Agent
Workflow before Automation
Validation before Claim
Approval before Side Effect
Demo before Expansion
```

最终判断标准不是代码量，也不是 Maven 模块是否都存在，而是：

> **一个真实用户、一个真实岗位、一个真实项目，能否在不伪造经历的前提下完整跑通 RoleOS Career Loop。**
