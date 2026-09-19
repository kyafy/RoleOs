# 001 — Career Foundation

> **Feature ID**：001  
> **Feature Name**：Career Foundation  
> **Product**：RoleOS  
> **Stage**：V1  
> **Status**：Revised after Cross-Feature Consistency Review

---

# 1. 目标

Career Foundation 是 RoleOS V1 的职业数据与长流程基础。

它解决三个核心问题：

```text
用户是谁？
用户真实做过什么？
一个长期求职任务如何被保存、暂停、恢复和继续？
```

本 Feature 完成后，需要为后续 Job Intelligence、Experience Match、Experience Builder、Project Upgrade、Resume 和 Application 提供统一基础：

```text
Career Profile
+
Experience / Project
+
Skill / Capability
+
Fact Provenance
+
Career Workflow
+
Human Approval
```

核心原则：

> **职业事实必须结构化、可追溯、可长期保存；Agent Context 不能替代 Career Database。**

---

# 2. 用户与范围

RoleOS V1 以单用户使用为目标，但核心领域模型从第一天保留：

```text
userId
```

因此：

```text
V1 Product Experience = Single User
V1 Domain Model        = User-aware
```

本 Feature 包含：

- Career Profile；
- 手动资料维护；
- Resume Import；
- Experience；
- Project；
- Skill / Capability；
- Fact Provenance；
- Career Workflow Foundation；
- Human Approval；
- 基础 Web UI；
- 数据持久化。

本 Feature 不包含：

- Job Discovery；
- JD Requirement Extraction；
- Experience Match；
- Experience Mining 完整流程；
- Project Upgrade；
- Claim-Evidence Validation；
- Resume Compiler；
- Application；
- Outcome Feedback；
- Multi-user / Tenant / RBAC；
- Career Knowledge Graph。

---

# 3. 核心 User Stories

## US-001-01 Career Profile

作为 RoleOS 用户，我希望建立和维护自己的 Career Profile，以便后续所有 Job Match 和 Career Intelligence 能基于真实职业信息工作。

## US-001-02 Resume Import

作为已有简历的用户，我希望上传现有 Resume，由 RoleOS 提取 Experience、Project、Skill 等信息，并由我确认后写入 Career Profile。

## US-001-03 Skill & Capability

作为用户，我希望系统能够区分我主动声明的 Skill、从 Experience 中识别出的 Skill、以及未来被 Evidence 验证的 Skill，从而避免把“自我评价”与“真实验证”混为一谈。

## US-001-04 Durable Workflow

作为用户，我希望 RoleOS 的长期任务可以跨 Session、跨服务重启继续执行，并在需要用户确认时暂停等待。

---

# 4. Career Profile Model

Career Profile 是 RoleOS 的长期职业事实资产，不等同于 Resume。

```text
Career Profile
≠
Resume
```

Career Profile 至少包含：

```text
CareerProfile
├── userId
├── Basic Profile
├── Target Roles
├── Job Preferences
├── Experiences
├── Projects
├── Skills
├── Capabilities
└── Metadata
```

基础信息可包括：

- 姓名；
- 当前职业方向；
- 工作年限；
- 当前城市；
- 联系方式；
- 简介。

## 4.1 Career Profile 与 Career Master Profile 边界

`CareerProfile` 是长期职业事实的 Source of Truth，负责保存经过真实性规则约束的核心 Career Facts：

```text
Experience
Project
Skill
Capability
Target Role
Job Preference
```

Feature 006 中出现的：

```text
CareerMasterProfile
```

不是第二套事实数据库，而是一个 Logical Read Model / Aggregated View：

```text
CareerMasterProfile
=
CareerProfile
+
Claim
+
Evidence
+
Interview Asset
```

必须保持：

```text
CareerMasterProfile 不复制并独立维护 Career Facts
```

任何事实修改仍然回到 `CareerProfile / Career Fact` 的真实性规则。

Target Role 和 Skill 在 V1 中允许自由创建，不要求预先建立完整 Taxonomy。

---

# 5. Career Profile 初始化

Career Profile 支持两种初始化方式：

```text
Manual Input
+
Resume Import
```

## 5.1 手动录入

用户自己直接填写或修改的信息：

```text
默认视为 USER_CONFIRMED
```

并保留：

```text
source = USER_INPUT
```

无需额外二次确认。

## 5.2 Resume Import

Resume Import 流程：

```text
Resume
→ Agent Extraction
→ Fact Candidate
→ User Review
→ Confirm / Edit / Reject
→ Career Fact
```

Agent 从 Resume 中提取的信息不能直接成为最终 Career Fact。

基础 Web UI 必须支持：

```text
Confirm
Edit
Reject
```

---

# 6. Experience & Project

## 6.1 Experience

Experience 表示用户真实发生过的经历，至少支持：

```text
WORK_EXPERIENCE
PROJECT_EXPERIENCE
PERSONAL_PROJECT
OPEN_SOURCE_EXPERIENCE
LEARNING_EXPERIENCE
```

Experience 可以不完整，但必须真实。

后续 Experience Builder 再补充：

- Business Context；
- Problem；
- Decision；
- Result；
- Trade-off。

## 6.2 Project

Project 是 Experience 中可以独立讨论、验证和升级的工程单元。

必须保持：

```text
Portfolio Project ≠ Production Experience
Personal Project  ≠ Company Project
```

Project 可以关联：

```text
Experience
Skill
Capability
Repository
```

后续 Project Upgrade 不得改变 Project 的历史来源。

---

# 7. Skill & Capability Model

## 7.1 Skill

Skill 表示：

> **一个可以被声明、被 Experience 支撑、被 Evidence 验证的原子能力单元。**

例如：

```text
Java
Spring Boot
RAG
LangGraph
MCP
Hybrid Retrieval
Rerank
Evaluation
Playwright
```

V1 允许自由创建 Skill，并至少保留：

```text
displayName
normalizedName
```

用于基础去重和后续 Alias / Taxonomy 扩展。

---

## 7.2 Skill Source

同一个 Skill 可以同时拥有多个来源：

```text
SELF_DECLARED
EXPERIENCE_DERIVED
VERIFIED_BY_EVIDENCE
```

### SELF_DECLARED

用户主动声明：

```text
我会 LangGraph
```

可以直接记录，但：

```text
SELF_DECLARED ≠ VERIFIED
```

### EXPERIENCE_DERIVED

Agent 根据 Experience / Resume 识别出 Skill：

```text
Experience
→ Skill Candidate
→ User Confirmation
→ EXPERIENCE_DERIVED
```

因为 Agent 参与产生了新结构化结论，所以必须由用户确认。

### VERIFIED_BY_EVIDENCE

未来当 Source Code、Test、Evaluation 等 Evidence 存在时，Skill 可以获得验证来源。

001 只建立关系，不实现完整 Evidence Validation。

---

## 7.3 Skill Depth

Skill Depth 采用四档：

```text
AWARENESS
USED
WORKING
ADVANCED
```

含义：

| Depth | 定义 |
|---|---|
| AWARENESS | 理解基本概念，但没有足够实践 |
| USED | 在学习或项目中实际使用过 |
| WORKING | 能独立使用该 Skill 完成实际功能 |
| ADVANCED | 能处理复杂问题并解释设计与 Trade-off |

必须区分：

```text
Self-assessed Depth
≠
Verified Depth
```

例如：

```text
LangGraph

Self-assessed: ADVANCED
Verified: WORKING
```

001 允许用户填写 Self-assessed Depth，但不实现完整 Verified Depth 推导算法。

---

## 7.4 Capability

Capability 是多个 Skill 与 Experience 组合形成的高层解决问题能力。

例如：

```text
RAG Engineering
AI Agent Engineering
Backend Engineering
LLMOps
```

关系：

```text
Capability
↔ Skill
↔ Experience / Project
```

001 只建立基本关系，不实现：

- 自动 Capability 推导；
- Capability Score；
- Capability Graph；
- Capability Match Algorithm。

这些在 Feature 003 中处理。

---

# 8. Fact Provenance

RoleOS 必须始终区分：

```text
User Fact
Agent Inference
Designed Scenario
Generated Content
Verified Evidence
```

核心规则：

### 用户直接输入

```text
USER_INPUT
→ USER_CONFIRMED
```

不要求再次确认。

### Agent 参与产生的新事实

任何由 Agent：

- 提取；
- 总结；
- 推断；
- 转换成结构化事实；

产生的新 Career Fact Candidate 都必须先进入：

```text
WAITING_CONFIRMATION
```

用户确认后才能成为 Career Fact。

### Agent 推断

```text
AGENT_INFERENCE
≠ Career Fact
```

未经用户确认或 Evidence 验证，不得覆盖已有 User Fact。

### Narrative

Agent 对已确认事实进行文案优化时：

```text
Generated Narrative
≠ New Career Fact
```

---

# 9. Career Database 与 Memory 边界

必须遵守：

> **Database 管事实，Memory 管上下文。**

Career Database 保存：

```text
Career Profile
Experience
Project
Skill
Capability
Fact Provenance
Workflow
Approval
```

Agent Memory 保存：

```text
用户偏好
对话上下文
交互经验
非关键语义记忆
```

禁止：

```text
Memory:
“用户应该熟悉 MCP”

→ 自动写入 CareerProfile:
MCP = ADVANCED
```

Memory 不是 Career Fact Source of Truth。

---

# 10. Career Workflow & Human Approval

## 10.1 Workflow 定义

Career Workflow 是：

> **一个可以跨 Session、跨服务重启、跨时间持续执行的业务流程。**

必须保持：

```text
Agent Session ≠ Career Workflow
```

001 只建立三个 Workflow Type：

```text
BROAD_APPLY
TARGETED_APPLY
PROJECT_UPGRADE
```

但不实现后续完整业务 Stage。

---

## 10.2 Workflow Lifecycle

通用生命周期：

```text
CREATE
START
WAIT
PAUSE
RESUME
RETRY
COMPLETE
CANCEL
```

状态至少支持：

```text
RUNNING
WAITING_USER_INPUT
WAITING_APPROVAL
PAUSED_FOR_HUMAN
RETRYABLE_FAILED
FAILED
COMPLETED
CANCELLED
```

以上枚举属于：

```text
CareerWorkflowStatus
```

它只描述“长流程当前为什么在运行、等待、暂停或失败”，不得与具体业务对象的生命周期状态混为一谈。

例如：

```text
ProjectUpgrade.status = IN_PROGRESS
CareerWorkflow.status = WAITING_USER_INPUT
```

表示 Upgrade 业务仍处于执行阶段，但 Workflow 因缺少 API Key 等用户输入而暂停。

后续 Feature 可以拥有自己的领域状态，例如：

```text
ProjectUpgradeStatus
ResumeVersionStatus
ApplicationStatus
```

这些领域状态不能反向替代 `CareerWorkflowStatus`。

Workflow 状态必须持久化。

服务重启后必须能够恢复：

```text
Workflow Type
Current Stage
Current Status
Waiting Reason
Context
Retry Information
```

---

## 10.3 Workflow Transition

核心规则：

> **Agent 不能直接修改 Workflow Stage。**

必须经过：

```text
Agent Decision
→ Workflow Transition Rule
→ Validation
→ Persist Transition
```

---

## 10.4 Human Decision / Approval Foundation

001 建立统一的人类决策基础机制，而不是要求所有 Feature 共用同一个业务 Decision Enum。

V1 预留 Subject Type：

```text
EXPERIENCE_CONFIRMATION
PROJECT_UPGRADE_APPROVAL
RESUME_APPROVAL
APPLICATION_APPROVAL
```

001 重点使用：

```text
EXPERIENCE_CONFIRMATION
```

统一 Envelope 至少应表达：

```text
HumanDecision
├── decisionType
├── subjectType
├── subjectId
├── status
├── payload
└── decidedAt
```

对于通用 Approval 场景，可以使用：

```text
APPROVE
REJECT
EDIT_REQUESTED
DEFER
```

但后续 Feature 允许定义更符合领域语义的 Decision，例如：

```text
UpgradeResultDecision:
ACCEPT / REQUEST_FIX / RETRY / ABORT

ResumeReviewDecision:
ACCEPT / EDIT / REJECT / REQUEST_REWRITE

ApplicationDecision:
APPROVE / EDIT / SKIP / DEFER
```

这些都是 `HumanDecision` 的不同业务 payload，不应被强制压成一个全局枚举。

当某个决策是 Workflow 的阻塞点：

```text
CareerWorkflow.status
→ WAITING_APPROVAL
```

只有用户完成对应 Human Decision 后才能继续。

---

# 11. Basic Web UI

001 明确要求提供基础 Web UI，而不是只提供 API。

至少包含四类页面或等价交互入口。

## 11.1 Career Profile

支持：

- 查看 / 编辑 Profile；
- 添加 / 编辑 Experience；
- 添加 / 编辑 Project。

## 11.2 Resume Import

支持：

```text
Upload
→ Extract
→ Review
→ Confirm / Edit / Reject
```

## 11.3 Skill / Capability

至少展示：

```text
Skill Name
Self-assessed Depth
Sources
Supporting Experience
Verified Depth / Verification Status
```

## 11.4 Workflow / Approval

至少展示：

```text
Workflow Type
Current Stage
Current Status
Waiting Reason
Updated Time
```

并允许用户：

```text
Resume
Cancel
Approve
Edit
Reject
Defer
```

具体可用操作应根据当前状态决定。

---

# 12. Core Business Rules

以下规则属于 001 的核心不变量。

```text
R-001
Agent Session ≠ Career Workflow
```

```text
R-002
Agent Inference ≠ User Fact
```

```text
R-003
Self Declared Skill ≠ Verified Skill
```

```text
R-004
Self-assessed Depth ≠ Verified Depth
```

```text
R-005
Portfolio Project ≠ Production Experience
```

```text
R-006
Memory ≠ Career Database
```

```text
R-007
Agent Decision ≠ Workflow Transition
```

```text
R-008
Agent-generated Career Fact Candidate
must be confirmed before becoming Career Fact
```

此外：

- 所有核心 Domain Object 必须具备明确 `userId` 归属；
- 服务重启后 Career Fact、Workflow、Approval 不得丢失；
- Resume 和 Career Profile 等敏感数据不得完整写入普通日志；
- Agent 生成的结构化信息必须保留来源；
- Career Foundation 不绑定 Job Site、Browser Provider、Coding Agent 或 Resume Renderer。

---

# 13. Acceptance Scenarios

## AC-001 — 手动建立 Career Profile

用户可以：

```text
创建 Career Profile
添加 Experience
添加 Project
添加 Skill: Java
```

结果：

```text
数据持久化成功
Java Source = SELF_DECLARED
所有对象属于当前 userId
```

---

## AC-002 — Resume Import

```text
上传 Resume
→ Agent 提取 Experience / Project / Skill
→ 生成 Fact Candidate
→ 用户 Confirm / Edit / Reject
```

只有确认后的内容才能进入 Career Profile。

---

## AC-003 — Skill 多来源与 Depth

用户先声明：

```text
LangGraph = ADVANCED
```

则：

```text
Self-assessed Depth = ADVANCED
Source contains SELF_DECLARED
```

后续 Experience 证明用户实际使用 LangGraph 并经用户确认：

```text
Source contains:
SELF_DECLARED
EXPERIENCE_DERIVED
```

如果当前没有足够 Evidence：

```text
Verified Depth
不得自动等于 ADVANCED
```

---

## AC-004 — Agent Inference 不能成为 Fact

Agent 推断：

```text
用户可能熟悉 MCP
```

在用户未确认、也没有 Evidence 的情况下：

```text
Career Profile
不得出现 Verified MCP Skill
```

---

## AC-005 — Workflow Recovery

```text
TARGETED_APPLY Workflow
→ WAITING_APPROVAL
→ RoleOS Restart
→ User Returns
```

结果：

```text
Workflow 保持原状态
用户能够继续处理 Approval
无需重新开始
```

---

## AC-006 — Agent 不能跳过 Approval

当 Workflow 为：

```text
WAITING_APPROVAL
```

即使 Agent 建议进入下一阶段：

```text
Workflow 也不能自动继续
```

直到用户提交 Approval Decision。

---

## AC-007 — Basic Web UI

用户必须能够通过 Web UI 完成：

```text
Career Profile 管理
Resume Import
Agent 提取结果确认
Experience / Project 管理
Skill / Capability 查看与维护
Workflow 查看
Approval 处理
```

---

# 14. Edge Cases

### Resume Extraction Error

如果 Agent 将：

```text
“了解 MCP”
```

误识别成：

```text
MCP ADVANCED
```

用户必须能够 Edit / Reject，错误 Candidate 不得进入 Career Fact。

### Skill Duplicate

对于：

```text
LangGraph
langgraph
Lang Graph
```

系统应具备基础 normalizedName 能力。

001 不要求复杂 Alias Engine。

### Incomplete Experience

缺少 Result、Business Context 等信息的真实 Experience 可以保存。

后续 Feature 004 再进行 Experience Mining。

### Deferred Approval

`DEFERRED` 不等于 Rejected。

Workflow 必须保持可恢复状态。

### Resume Partial Extraction

部分字段解析失败时：

```text
成功部分仍可 Review
失败部分明确提示
```

不得虚构缺失字段。

---

# 15. Definition of Done

001 Career Foundation 完成必须满足：

- [ ] Career Profile 可持久化；
- [ ] 核心 Domain Model 保留 `userId`；
- [ ] 支持手动初始化 Career Profile；
- [ ] 支持 Resume Import + User Review；
- [ ] Experience / Project 可独立维护；
- [ ] Skill 可自由创建；
- [ ] Skill 支持多来源；
- [ ] Skill Depth 使用 AWARENESS / USED / WORKING / ADVANCED；
- [ ] Self-assessed Depth 与 Verified Depth 分离；
- [ ] Capability 与 Skill / Experience / Project 建立基础关系；
- [ ] Fact Provenance 可追踪；
- [ ] Career Database 与 Agent Memory 边界明确；
- [ ] Career Workflow 可持久化和恢复；
- [ ] CareerWorkflowStatus 与 ProjectUpgrade / ResumeVersion / Application 等领域 Status 明确分层；
- [ ] Human Approval 可以阻塞和恢复 Workflow；
- [ ] Agent 不能直接修改 Workflow Stage；
- [ ] HumanDecision 作为统一决策 Envelope，后续 Feature 可拥有自己的领域 Decision；
- [ ] CareerMasterProfile 被定义为 Read Model，而不是第二套 Career Fact Source of Truth；
- [ ] 基础 Web UI 可完成核心操作；
- [ ] Acceptance Scenarios 通过；
- [ ] 未提前实现 Feature 002～007 的业务能力。

最终验收 Demo：

```text
进入 RoleOS
→ 上传已有 Resume
→ Agent 提取职业信息
→ 用户确认 / 修改
→ 建立 Career Profile
→ 补充 Project
→ 声明 Skill
→ Agent 从 Experience 识别新 Skill
→ 用户确认
→ 创建 Career Workflow
→ WAITING_APPROVAL
→ 重启 RoleOS
→ Workflow 恢复
→ 用户 Approve
→ Workflow 继续
```

如果这条链路稳定运行：

> **001 Career Foundation 验收通过。**
