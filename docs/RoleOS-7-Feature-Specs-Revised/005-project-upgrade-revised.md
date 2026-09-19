# 005 — Project Upgrade

> **Feature ID**：005  
> **Feature Name**：Project Upgrade  
> **Product**：RoleOS  
> **Stage**：V1  
> **Status**：Revised after Cross-Feature Consistency Review

---

# 1. 目标

Project Upgrade 是 RoleOS V1 中负责“真实补能力”的执行层。

它解决的问题不是：

```text
怎么把简历写得更像会某项技能
```

而是：

```text
用户当前确实存在 Skill Gap
→ 是否值得补
→ 用哪个真实项目补
→ 应该改什么
→ 如何验证真的补上了
→ 能留下什么 Evidence
```

核心流程：

```text
Skill Gap
→ Upgrade Candidate
→ Project Selection
→ Value Assessment
→ Upgrade Plan
→ Validation Contract
→ Human Approval
→ Codex Execution
→ Independent Validation
→ Completed / Partial / Failed
→ Verified Capability / Evidence
```

核心原则：

> **Project Upgrade 的目标是让能力真实成立，而不是把过去没有发生过的事情写进历史经历。**

---

# 2. 范围

005 负责：

- 基于 Gap / Upgrade Feasibility 创建并评估 Upgrade Candidate；
- Project Candidate 选择；
- Existing Project / New Project 支持；
- Upgrade Value Assessment；
- 多个强相关 Gap 合并；
- Upgrade Plan；
- Validation Contract；
- Human Approval；
- Codex 执行；
- CareerWorkflow WAITING_USER_INPUT / WAITING_APPROVAL / PAUSED_FOR_HUMAN；
- API Key / 配置等 Human Input；
- Retry / Resume / Abort；
- Independent Validation；
- Completed / Partial / Failed；
- Technical Evidence Build 与 Evidence Artifact 产出；
- Verified Capability 更新；
- Project Upgrade Web Workspace。

本 Feature 不负责：

- Job Discovery；
- Experience Mining；
- Resume Compiler；
- Application Submit；
- 将新增能力伪装成历史任职事实；
- 自动接受 Codex 输出；
- 无验证依据的“企业级”“高性能”“显著提升”等 Claim。

---

# 3. 核心 User Stories

## US-005-01 — 选择值得补的 Gap

作为用户，我希望 RoleOS 不会对所有 Skill Gap 都自动投入开发成本，而是先判断价值、成本、可行性和可验证性。

## US-005-02 — 基于真实项目补能力

作为用户，我希望 RoleOS 可以在已有真实项目上升级能力；如果没有合适项目，也可以新建一个真实项目来补 Skill Gap。

## US-005-03 — 执行前明确方案

作为用户，我希望在 Codex 修改项目之前，先看到明确的 Upgrade Plan、Deliverables、Validation Plan 和 Expected Evidence，并由我确认。

## US-005-04 — 中断与恢复

作为用户，我希望升级过程中如果缺少 API Key、配置或人工输入，Workflow 可以暂停并提示我补充，而不是直接失败。

## US-005-05 — 独立验证

作为用户，我希望 Codex 说“完成”以后，RoleOS 仍然独立验证代码、功能、Requirement 和 Evidence，再决定 Upgrade 是否真的完成。

---

# 4. Project Provenance & Timeline

005 不用“新项目 / 旧项目”简单判断项目真实性。

必须区分三个维度：

```text
Project Context
+
Project Origin
+
Upgrade State
```

---

## 4.1 Project Context

表示项目最初属于什么场景：

```text
WORK_PROJECT
PERSONAL_PROJECT
OPEN_SOURCE_PROJECT
LEARNING_PROJECT
```

---

## 4.2 Project Origin

表示项目在进入 RoleOS 之前是否已经真实存在：

```text
HISTORICAL_EXISTING
CREATED_FOR_UPGRADE
```

导入过去做过的 Repo：

```text
Import
≠
New Project
```

如果项目在过去已经真实存在，即使今天第一次导入 RoleOS：

```text
Origin = HISTORICAL_EXISTING
```

---

## 4.3 Upgrade State

表示当前项目是否被 RoleOS 后续升级：

```text
ORIGINAL
UPGRADED
```

例如：

```text
Project Context:
WORK_PROJECT

Project Origin:
HISTORICAL_EXISTING

Upgrade State:
UPGRADED
```

表示：

> 这个项目过去真实存在于工作经历中，但今天又通过 RoleOS 增加了新的能力。

---

## 4.4 核心时间边界

必须保持：

```text
Current Verified Capability
≠
Historical Work Fact
```

例如：

```text
2024:
公司项目没有 MCP

2026:
RoleOS 在该 Repo 上加入 MCP 并完成验证
```

那么：

```text
2026 年具备 MCP Capability
= 可以成立
```

但不能自动改写为：

```text
2024 年在公司项目中已经具备 MCP 生产实践
```

---

# 5. Upgrade Candidate & Evidence Build Input

005 是正式 `UpgradeCandidate` 的 Owner。

输入可以来自：

```text
003:
Gap + UpgradeFeasibility

004:
Refined Gap + Upgrade Signal + Evidence Requirement
```

005 基于这些输入创建：

```text
UpgradeCandidate
├── Target Requirement
├── Current Gap
├── Related Skills
├── Candidate Projects
├── Expected Capability
└── Reason
```

以下 SKILL_GAP 适合进入 Upgrade Assessment：

```text
SKILL_GAP
+
Upgrade Feasibility != NOT_APPLICABLE
```

此外，005 负责需要“真实工程执行”才能产生的新 Evidence：

```text
Technical Evidence Requirement
→ Test / Eval / Trace / Benchmark / Demo Task
→ EvidenceArtifact
```

因此某些 `EVIDENCE_GAP` 即使不需要新增 Skill，也可以进入 005 的 Technical Evidence Build；005 不负责历史材料的人工真实性确认，那部分由 006 处理。

---

# 6. Project Selection

005 支持两种载体。

## 6.1 Existing Project Upgrade

优先选择已有项目。

例如：

```text
已有 RAG Project
缺：
Rerank
Evaluation
```

可以：

```text
Existing RAG Project
→ Upgrade
```

优点：

- 与已有 Experience 连续；
- 更容易形成真实技术演进；
- 更容易证明“Before / After”。

---

## 6.2 New Project

当现有项目不适合作为载体时，可以：

```text
Create New Project
```

例如：

```text
Gap:
MCP Server / Client
```

而现有项目都不适合承载，则可以新建：

```text
MCP Agent Gateway
```

这个项目产生的 Skill / Capability / Evidence 都可以是真实的。

但其 Provenance 必须保留：

```text
CREATED_FOR_UPGRADE
```

后续 Claim / Resume 根据真实来源表达。

---

# 7. Upgrade Value Assessment

真正执行之前，RoleOS 必须判断：

```text
Value
Cost
Feasibility
Verifiability
```

---

## 7.1 Value

回答：

> 这个 Gap 对当前 Target Job / Target Role 有多大价值？

---

## 7.2 Cost

考虑：

- 代码改造量；
- 学习成本；
- 外部依赖；
- 基础设施；
- 时间成本；
- 运行成本。

---

## 7.3 Feasibility

回答：

> 当前项目是否适合作为能力升级载体？

---

## 7.4 Verifiability

回答：

> 做完之后能不能留下足够客观的验证结果和 Evidence？

---

## 7.5 Decision

RoleOS 可以给出：

```text
Recommended
Not Recommended
Needs Review
```

但最终是否投入由用户决定。

---

# 8. Multiple Gap Upgrade

一个 Upgrade Plan 可以同时解决多个 Gap，但必须满足：

> **属于同一业务目标，并且技术上高度相关。**

例如：

```text
RAG Quality Upgrade

Gap 1:
Hybrid Retrieval

Gap 2:
Rerank

Gap 3:
Evaluation
```

可以合并。

但不应该把：

```text
MCP
+
Frontend Design
+
Database Sharding
```

强行放进一个 Upgrade。

---

# 9. Upgrade Plan

执行前必须生成结构化 Upgrade Plan。

至少包含：

```text
UpgradePlan
├── Target Requirement
├── Target Gap
├── Target Project
├── Goal
├── Scope
├── Tasks
├── Expected Deliverables
├── Validation Contract
├── Expected Evidence
├── Risk
└── Human Input Requirements
```

---

## 9.1 Goal

必须说明：

```text
升级完成后，哪项真实能力应该成立？
```

---

## 9.2 Scope

必须定义：

```text
做什么
不做什么
```

避免把一个 Upgrade 无限扩张成“企业级重构”。

---

## 9.3 Deliverables

例如：

```text
Source Code
Tests
Evaluation
Configuration
Architecture Notes
Demo
```

Deliverable 必须与 Gap 相关。

---

# 10. Human Approval

005 使用两阶段 Human Decision。

两类决策都通过 001 的 `HumanDecision` Envelope 承载，但保持各自领域语义：

```text
UpgradePlanDecision
UpgradeResultDecision
```

---

## 10.1 Upgrade Plan Approval

Codex 执行前：

```text
ProjectUpgrade.status = PLANNED
CareerWorkflow.status = WAITING_APPROVAL
```

用户可以：

```text
Approve
Edit
Reject
Defer
```

未经批准：

```text
不得修改真实 Repo
```

---

## 10.2 Result Review

Validation 完成后，用户可以：

```text
Accept
Request Fix
Retry
Abort
```

Codex 的执行结果不能自动被用户“接受”。

---

# 11. Execution & Codex Boundary

RoleOS 决定：

```text
为什么升级
升级什么
做到什么程度
怎样算完成
需要什么 Evidence
```

Codex 决定：

```text
具体怎么实现
```

必须保持：

```text
RoleOS
→ Structured Coding Task
→ Codex
```

而不是：

```text
“帮我把项目升级成企业级”
```

这样的模糊指令。

---

# 12. Human Input & Secure Pause

升级过程中允许因为缺少用户输入而暂停。

例如：

```text
API Key
Test Account
Configuration
Business Constraint
External Service Credential
```

流程采用双层状态：

```text
ProjectUpgrade.status = IN_PROGRESS
CareerWorkflow.status = RUNNING

→ Missing Required Input

ProjectUpgrade.status = IN_PROGRESS
CareerWorkflow.status = WAITING_USER_INPUT

→ Human Input
→ Resume

ProjectUpgrade.status = IN_PROGRESS
CareerWorkflow.status = RUNNING
```

---

## 12.1 WAITING_USER_INPUT

`WAITING_USER_INPUT` 属于 `CareerWorkflowStatus`，不是 `ProjectUpgradeStatus`。

适用于：

```text
需要用户提供新信息或运行凭证
```

例如：

```text
OPENAI_API_KEY
```

---

## 12.2 WAITING_APPROVAL

`WAITING_APPROVAL` 属于 `CareerWorkflowStatus`。

适用于：

```text
需要用户批准 Upgrade Plan / Side Effect
```

---

## 12.3 PAUSED_FOR_HUMAN

`PAUSED_FOR_HUMAN` 属于 `CareerWorkflowStatus`。

适用于：

```text
外部人工操作
Captcha
平台安全验证
无法自动处理的人工步骤
```

---

## 12.4 Secret Handling

API Key 等 Secret 不得进入：

```text
Career Fact
Agent Memory
普通 Workflow Context
普通 Log
```

应通过安全输入路径传递给：

```text
Runtime / Process Environment
```

任务完成后不默认持久化明文 Secret。

---

# 13. Upgrade Status

V1 使用领域状态：

```text
ProjectUpgradeStatus:
PLANNED
APPROVED
IN_PROGRESS
VALIDATING
COMPLETED
PARTIAL
FAILED
CANCELLED
```

等待用户输入、审批或人工接管时，使用独立的 `CareerWorkflowStatus`，不向 `ProjectUpgradeStatus` 增加 WAITING 状态。

关键规则：

```text
Codex Completed
≠
Upgrade COMPLETED
```

Codex 执行结束后：

```text
→ VALIDATING
```

只有验证通过，才进入最终状态。

---

# 14. Validation Contract

Validation Contract 必须在执行前定义。

它回答：

> **怎样证明这个 Upgrade 真的完成？**

验证层级：

```text
L1 Build / Runnable
L2 Functional
L3 Requirement-specific
L4 Evaluation / Benchmark
L5 Evidence
```

---

## 14.1 L1 — Build / Runnable

回答：

> 项目还能不能正常构建和运行？

例如：

```text
Build PASS
Application Starts
Required Module Loads
```

绝大多数 Upgrade 都需要。

---

## 14.2 L2 — Functional Validation

回答：

> 新增功能有没有真的工作？

例如 MCP：

```text
Server starts
Tool discovered
Tool invoked
Expected result returned
```

不能只判断：

```text
代码里出现了某个 Class
```

---

## 14.3 L3 — Requirement-specific Validation

回答：

> 当前 Gap 本身有没有被补齐？

例如 Requirement：

```text
Agent Workflow Error Recovery
```

验证应围绕：

```text
Tool Failure
→ Retry
→ Checkpoint
→ Resume
→ Expected State
```

Validation 必须和 Requirement 对齐。

---

## 14.4 L4 — Evaluation / Benchmark

当 Claim 涉及：

```text
效果
准确率
召回率
质量
延迟
吞吐
稳定性
成本
```

必须有 Evaluation / Benchmark。

例如：

```text
“加入 Reranker 提升检索质量”
```

必须存在：

```text
Baseline
vs
Upgraded Version
```

才能产生质量提升 Claim。

必须保持：

```text
Feature Works
≠
Feature Improves Quality
```

---

## 14.5 L5 — Evidence Validation

最终必须确认能留下什么 Evidence，例如：

```text
Source Code
Git Commit
Test Result
Evaluation Report
Trace
Architecture Document
Demo
```

没有 Evidence 的结果，不应直接进入高置信 Claim。

---

# 15. Validation Requirement Level

每个 Upgrade 的每一层 Validation 可以标记：

```text
REQUIRED
OPTIONAL
NOT_APPLICABLE
```

例如 MCP：

```text
Build                 REQUIRED
Functional            REQUIRED
Requirement-specific  REQUIRED
Evaluation            NOT_APPLICABLE
Evidence               REQUIRED
```

RAG Quality Upgrade：

```text
Build                 REQUIRED
Functional            REQUIRED
Requirement-specific  REQUIRED
Evaluation            REQUIRED
Evidence               REQUIRED
```

---

# 16. Independent Validation

RoleOS 不能完全依赖 Codex 自己声明：

```text
“测试通过”
```

必须基于：

```text
真实 Build
真实 Test
真实 Eval
真实 Artifact
```

执行独立验证。

---

# 17. Completion Semantics

## 17.1 COMPLETED

只有：

```text
所有 Mandatory Deliverables
+
所有 Required Validation
+
Required Evidence
```

全部通过，才可以：

```text
COMPLETED
```

---

## 17.2 PARTIAL

例如：

```text
Hybrid Retrieval PASS
Rerank PASS
Evaluation FAILED
```

则：

```text
Upgrade Status = PARTIAL
```

只允许更新已经验证成功的能力。

例如可以新增：

```text
Hybrid Retrieval
Rerank
```

但不能新增：

```text
Evaluation = VERIFIED
```

也不能产生：

```text
“通过 Evaluation 证明检索质量显著提升”
```

---

## 17.3 FAILED

如果核心 Mandatory Validation 失败：

```text
FAILED
```

不能产生 Verified Capability。

允许：

```text
Retry
Request Fix
Abort
```

---

# 18. Skill / Capability Update

Upgrade 完成后，只能根据真实 Validation Result 更新：

```text
Skill
Capability
Verified Depth
Evidence Link
```

不能根据：

```text
Plan
Codex Output
Agent Opinion
```

直接升级 Career Profile。

例如：

```text
Codex 实现 MCP Server
+
Functional Test PASS
+
Requirement Validation PASS
+
Evidence 存在
```

才能支撑：

```text
MCP Capability Verified
```

---

# 19. Project Upgrade Web Workspace

005 提供 Project Upgrade Workspace。

至少展示：

```text
Target Job
Target Requirement
Current Gap
Upgrade Candidate
Target Project
Project Provenance
Upgrade Assessment
Upgrade Plan
Validation Contract
Approval
Execution Status
Human Input Request
Validation Result
Evidence
Result Review
```

---

## 19.1 Plan Review

用户可以查看：

```text
Goal
Scope
Tasks
Deliverables
Validation
Evidence
Risk
```

并：

```text
Approve
Edit
Reject
Defer
```

---

## 19.2 Execution

执行过程中显示：

```text
Current Status
Current Task
Completed Tasks
Pending Tasks
Waiting Reason
```

如果缺用户输入：

```text
显示 Human Input Popup
```

---

## 19.3 Validation Result

至少展示：

```text
Build
Functional
Requirement Validation
Evaluation
Evidence
```

每项：

```text
PASS
FAIL
SKIPPED
NOT_APPLICABLE
```

---

## 19.4 Result Review

用户可以：

```text
Accept
Request Fix
Retry
Abort
```

并查看：

```text
升级前能力
→ 做了什么
→ 验证结果
→ 哪些 Skill / Capability 被真实提升
```

---

# 20. Core Business Rules

```text
R-001
Import Existing Project ≠ Create New Project
```

导入只是接入方式。

```text
R-002
Current Verified Capability ≠ Historical Work Fact
```

新增能力不能倒灌历史。

```text
R-003
Upgrade must target a real Gap
```

不做无目标升级。

```text
R-004
Multiple Gaps may be grouped only when strongly related
```

避免 Scope 膨胀。

```text
R-005
Value / Cost / Feasibility / Verifiability before execution
```

先判断是否值得做。

```text
R-006
Human Approval before repo modification
```

没有批准不得执行。

```text
R-007
Missing Human Input → CareerWorkflowStatus.WAITING_USER_INPUT
```

`ProjectUpgradeStatus` 保持当前业务阶段，不直接 FAILED。

```text
R-008
Secret ≠ Career Data
```

API Key 不进入 Career Fact / Memory / 普通日志。

```text
R-009
Codex Output ≠ Upgrade Completed
```

Codex 完成只进入 VALIDATING。

```text
R-010
Validation must align with Requirement
```

测试不能与 Gap 无关。

```text
R-011
Feature Works ≠ Feature Improves Quality
```

效果提升 Claim 必须有 Evaluation。

```text
R-012
Partial Result only upgrades verified capability
```

失败部分不得进入 Verified Skill / Claim。

```text
R-013
Technical Evidence Build belongs to 005
```

需要执行 Test / Eval / Trace / Benchmark / Demo 才能产生的新 Evidence，由 005 输出 `EvidenceArtifact`；已有历史材料的接入与 Claim 关联由 006 处理。

---

# 21. Acceptance Scenarios

## AC-001 — Imported Historical Project

Given：

```text
用户导入一个过去真实存在的 RAG Repo
```

Then：

```text
Origin = HISTORICAL_EXISTING
```

导入行为本身：

```text
不等于 Upgrade
```

---

## AC-002 — Existing Project Upgrade

Given：

```text
已有 RAG Project
Gap = Rerank
```

When：

```text
用户批准 Upgrade Plan
Codex 完成修改
Validation 全部通过
```

Then：

```text
Upgrade = COMPLETED
Rerank Skill / Capability 可以获得验证来源
```

---

## AC-003 — New Project Upgrade

Given：

```text
Gap = MCP
没有合适 Existing Project
```

When：

```text
用户批准创建新项目
```

Then：

```text
可以创建真实 New Project
Origin = CREATED_FOR_UPGRADE
```

完成验证后：

```text
MCP Capability 可以成为 Verified Capability
```

但不能写成过去公司项目已经做过 MCP。

---

## AC-004 — Related Gap Grouping

Given：

```text
Hybrid Retrieval
Rerank
Evaluation
```

属于同一 RAG Quality Goal。

Then：

```text
可以进入同一个 Upgrade Plan
```

---

## AC-005 — Human Approval

Given：

```text
Upgrade Plan 已生成
```

Then：

```text
ProjectUpgrade.status = PLANNED
CareerWorkflow.status = WAITING_APPROVAL
```

未经用户批准：

```text
Codex 不得修改 Repo
```

---

## AC-006 — API Key Pause

Given：

```text
Codex 执行 Evaluation
需要 OPENAI_API_KEY
```

Then：

```text
ProjectUpgrade.status = IN_PROGRESS
CareerWorkflow.status = WAITING_USER_INPUT
```

Web UI 提示用户安全输入。

用户输入后：

```text
Workflow Resume
```

Secret 不进入普通日志和 Career Fact。

---

## AC-007 — Codex Completion

Given：

```text
Codex 返回任务完成
```

Then：

```text
Upgrade → VALIDATING
```

不得直接：

```text
COMPLETED
```

---

## AC-008 — Evaluation Required

Given：

```text
Upgrade Claim:
“Reranker 提升检索质量”
```

Then：

必须有：

```text
Baseline
+
Upgraded Result
+
Evaluation Metric
```

否则不能生成质量提升 Claim。

---

## AC-009 — Partial Success

Given：

```text
Hybrid Retrieval PASS
Rerank PASS
Evaluation FAIL
```

Then：

```text
Upgrade = PARTIAL
```

只有：

```text
Hybrid Retrieval
Rerank
```

可以进入 Verified Capability。

---

## AC-010 — Historical Truth Boundary

Given：

```text
2024 公司项目没有 MCP
2026 RoleOS Upgrade 增加 MCP
```

Then：

系统可以确认：

```text
2026 用户具备 MCP 实践能力
```

但不得自动生成：

```text
2024 公司项目已使用 MCP
```

---

# 22. Edge Cases

### Upgrade Project Not Suitable

如果 Existing Project 与 Gap 关联很弱：

```text
不强行升级
```

可以推荐：

```text
New Project
或
Do Not Upgrade
```

---

### Validation Dependency Failure

如果外部服务不可用导致 Evaluation 无法执行：

```text
ProjectUpgrade.status = VALIDATING
```

根据原因更新 Workflow：

```text
缺用户输入
→ CareerWorkflow.status = WAITING_USER_INPUT

可重试外部依赖失败
→ CareerWorkflow.status = RETRYABLE_FAILED
```

根据原因处理。

不得默认 PASS。

---

### Existing Behavior Regression

如果新能力通过，但破坏已有核心行为：

```text
不能直接 COMPLETED
```

应进入：

```text
PARTIAL / FAILED
```

取决于 Validation Contract。

---

### Human Rejects Result

即使 Validation 全部通过：

```text
用户仍可以 Request Fix / Abort
```

系统保留事实和 Evidence，但不强制推进后续 Workflow。

---

### Unsupported Claim

如果代码功能存在，但没有足够 Evidence：

```text
Capability 可以保持低置信支持状态
```

不得升级为高置信 Verified Claim。

---

# 23. Definition of Done

005 Project Upgrade 完成必须满足：

- [ ] 支持导入 Historical Existing Project；
- [ ] Import 与 New Project 创建明确区分；
- [ ] 支持 Existing Project Upgrade；
- [ ] 支持 New Project 作为 Skill Gap 的真实补齐载体；
- [ ] Project Context / Origin / Upgrade State 可追踪；
- [ ] Current Verified Capability 与 Historical Work Fact 明确区分；
- [ ] Upgrade 前进行 Value / Cost / Feasibility / Verifiability 判断；
- [ ] 强相关 Gap 可以合并为同一 Upgrade Plan；
- [ ] Upgrade Plan 明确 Goal / Scope / Deliverables / Validation / Evidence；
- [ ] Codex 执行前必须 Human Approval；
- [ ] CareerWorkflowStatus 与 ProjectUpgradeStatus 明确分层；
- [ ] 执行过程中支持 CareerWorkflowStatus.WAITING_USER_INPUT；
- [ ] API Key 等 Secret 不进入 Career Fact / Memory / 普通 Log；
- [ ] Codex 完成后进入 VALIDATING，而不是直接 COMPLETED；
- [ ] Validation Contract 至少覆盖 Build / Functional / Requirement / Evaluation / Evidence；
- [ ] Validation 根据 Upgrade 类型选择 REQUIRED / OPTIONAL / NOT_APPLICABLE；
- [ ] 质量提升 Claim 必须有 Evaluation；
- [ ] 支持 COMPLETED / PARTIAL / FAILED；
- [ ] PARTIAL 只更新已验证能力；
- [ ] 完成结果可产生真实 EvidenceArtifact；
- [ ] Technical Evidence Gap 可以通过 Test / Eval / Trace / Benchmark / Demo Task 构建 Evidence；
- [ ] Verified Capability 只根据 Validation Result 更新；
- [ ] Web UI 支持 Plan Review / Approval / Execution / Human Input / Validation / Result Review；
- [ ] Acceptance Scenarios 通过；
- [ ] 未提前实现 Resume Compiler / Application。

最终验收 Demo：

```text
Target Job
→ SKILL_GAP
→ Upgrade Candidate
→ Existing / New Project Selection
→ Value Assessment
→ Upgrade Plan
→ Validation Contract
→ Human Approval
→ Codex Execution
→ Missing API Key
→ CareerWorkflow.status = WAITING_USER_INPUT
→ ProjectUpgrade = IN_PROGRESS
→ User Provides Secret
→ Resume
→ Codex Completes
→ VALIDATING
→ Build / Functional / Requirement / Eval / Evidence
→ COMPLETED or PARTIAL
→ Verified Capability
→ Evidence
```

如果 RoleOS 能够稳定回答：

```text
这个 Gap 值不值得补？
在哪个真实项目里补？
做完以后到底有没有真的补上？
新增能力以后应该怎么真实表达？
```

则：

> **005 Project Upgrade 验收通过。**
