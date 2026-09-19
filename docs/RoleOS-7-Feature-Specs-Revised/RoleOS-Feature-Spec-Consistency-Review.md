# RoleOS 7-Feature Spec 一致性检查报告

> 范围：001 Career Foundation ～ 007 Application & Feedback  
> 目的：在进入 `$speckit-plan` 前，统一跨 Feature 的领域语义、状态机、输入输出与职责边界。  
> 结论：整体业务主链路闭合，无需推翻重构；存在若干跨 Spec 一致性问题，应在 Plan 前修正。

---

# 1. 总体业务链路检查

当前 7 个 Feature 可以形成完整闭环：

```text
001 Career Foundation
    ↓
002 Job Intelligence
    ↓
003 JD & Experience Match
    ↓
004 Experience Builder
    ↓
005 Project Upgrade
    ↓
006 Claim-Evidence & Resume
    ↓
007 Application & Feedback
    ↓
Observation / Recommendation
    ↺
Job / Resume / Experience / Upgrade Strategy
```

核心真实性链路也基本一致：

```text
User Fact
→ Experience / Project
→ Match / Gap
→ Experience Mining or Project Upgrade
→ Evidence
→ Claim
→ Resume
→ Application
→ Outcome
```

以下问题主要属于“跨 Feature 接口定义不够统一”。

---

# 2. P0 — Plan 前必须修正

## P0-01 Workflow Status 与业务对象 Status 混用

### 当前问题

001 定义统一 Career Workflow Status：

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

但 005 Project Upgrade Status 又包含：

```text
WAITING_USER_INPUT
```

007 Application Status 又包含：

```text
WAITING_USER_INPUT
WAITING_APPROVAL
```

这会造成两个 Source of Truth：

```text
CareerWorkflow.status
vs
ProjectUpgrade.status / Application.status
```

### 建议

严格分层：

```text
CareerWorkflowStatus
= 流程执行状态

ProjectUpgradeStatus
= Upgrade 业务生命周期

ApplicationStatus
= Application 业务生命周期
```

推荐：

```text
CareerWorkflowStatus:
RUNNING
WAITING_USER_INPUT
WAITING_APPROVAL
PAUSED_FOR_HUMAN
RETRYABLE_FAILED
FAILED
COMPLETED
CANCELLED
```

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

```text
ApplicationStatus:
PREPARED
SUBMITTING
SUBMITTED
SUBMISSION_UNKNOWN
VIEWED
REPLIED
INTERVIEWING
REJECTED
OFFER
WITHDRAWN
CANCELLED
```

例如 API Key 缺失：

```text
ProjectUpgrade.status = IN_PROGRESS
CareerWorkflow.status = WAITING_USER_INPUT
```

而不是把两个状态合并。

---

## P0-02 Human Approval / Review Decision 枚举漂移

001 统一定义：

```text
APPROVED
REJECTED
EDIT_REQUESTED
DEFERRED
```

但：

005 Result Review 使用：

```text
Accept
Request Fix
Retry
Abort
```

006 Resume Review 使用：

```text
ACCEPT
EDIT
REJECT
REQUEST_REWRITE
```

007 Application Review 使用：

```text
APPROVE
EDIT
SKIP
DEFER
```

这些动作实际上不是同一种业务语义。

### 建议

不要强迫所有 Human Interaction 共用一个 Decision Enum。

建立统一 Envelope：

```text
HumanDecision
├── decisionType
├── subjectType
├── subjectId
├── status
├── payload
└── decidedAt
```

然后按场景使用不同 Decision：

```text
ApprovalDecision:
APPROVE
REJECT
EDIT_REQUESTED
DEFER
```

```text
UpgradeResultDecision:
ACCEPT
REQUEST_FIX
RETRY
ABORT
```

```text
ResumeReviewDecision:
ACCEPT
EDIT
REJECT
REQUEST_REWRITE
```

```text
ApplicationDecision:
APPROVE
EDIT
SKIP
DEFER
```

001 负责 Human Decision / Approval Foundation，后续 Feature 定义领域动作。

---

## P0-03 EVIDENCE_GAP 的下游职责存在断点

004 明确：

```text
EVIDENCE_GAP
→ Evidence Building
```

但 7 个 Feature 中没有独立 `Evidence Building` Feature。

005 可以产生技术 Evidence：

```text
Code
Test
Evaluation
Trace
Demo
```

006 又明确“不负责新 Evidence 的工程构建”。

因此当前存在：

```text
EVIDENCE_GAP
→ ?
```

### 建议

不增加第 8 个 Feature。

把 Evidence Gap 路由拆成两类：

```text
Technical / Project Evidence Gap
→ 005 Project Upgrade / Evidence Build Task
```

例如：

```text
缺 Test
缺 Eval
缺 Trace
缺 Benchmark
缺 Demo
```

以及：

```text
Historical / Existing Evidence Gap
→ 006 Evidence Intake & Claim Review
```

例如：

```text
已有截图
历史文档
Git Commit
用户提供的可验证材料
```

005 负责“产生新的工程 Evidence”。

006 负责“接收、验证、关联 Evidence 与 Claim”。

---

## P0-04 Match Status 与 Upgrade Feasibility 职责重叠

003 当前同时存在：

```text
MatchStatus:
MATCHED
PARTIAL_MATCH
UPGRADABLE
NOT_MATCHED
UNKNOWN
```

以及：

```text
UpgradeFeasibility:
HIGH
MEDIUM
LOW
NOT_APPLICABLE
```

`UPGRADABLE` 实际上已经是在表达 Upgrade Feasibility，因此两套字段容易产生冲突：

```text
Status = PARTIAL_MATCH
Feasibility = HIGH

还是

Status = UPGRADABLE
Feasibility = HIGH
```

### 建议

MatchStatus 只回答：

> 当前到底匹不匹配？

推荐：

```text
MATCHED
PARTIAL_MATCH
NOT_MATCHED
UNKNOWN
```

UpgradeFeasibility 单独回答：

> 缺口能不能通过 Project Upgrade 补齐？

```text
HIGH
MEDIUM
LOW
NOT_APPLICABLE
```

再通过 Workflow 派生：

```text
recommendedNextAction = PROJECT_UPGRADE
```

删除 `UPGRADABLE` Match Status。

---

## P0-05 Gap Taxonomy 出现未定义的 EXPERIENCE GAP

003 已经约定一级 Gap 只有：

```text
STORY_GAP
SKILL_GAP
EVIDENCE_GAP
```

但某个 Acceptance Scenario 又使用：

```text
SKILL / EXPERIENCE GAP
```

`EXPERIENCE_GAP` 并没有定义。

例如：

```text
要求 5 年真实管理经验
```

它又确实不完全等价于 Skill Gap。

### 建议

继续保留之前已经确认的“三种一级 Gap”，不要临时新增第四种。

增加第二个维度：

```text
RequirementNature
├── CAPABILITY
├── HISTORICAL_EXPERIENCE
├── CONTEXTUAL
└── EVIDENCE
```

例如：

```text
MatchStatus = NOT_MATCHED
RequirementNature = HISTORICAL_EXPERIENCE
UpgradeFeasibility = NOT_APPLICABLE
```

这样无需创造 `EXPERIENCE_GAP`，同时能够正确表达“5 年管理经验”这类历史条件。

---

## P0-06 004 与 006 对 Candidate Claim 的职责重叠

004 ExperienceAsset 当前包含：

```text
Candidate Claims
```

006 又声明：

```text
Candidate Claim 生成
Claim Validation
Claim Status
Claim Pool
```

因此可能出现：

```text
CandidateClaim 到底由 004 创建还是 006 创建？
```

### 建议

明确所有权：

```text
004
→ ClaimSuggestion
```

它只是从 Experience Mining 发现：

```text
“这段经历可能值得这样表达”
```

不创建正式 Claim。

006 唯一拥有：

```text
CandidateClaim
ClaimStatus
ClaimProvenance
ClaimPool
```

链路：

```text
ExperienceAsset.ClaimSuggestion
        ↓
006 Claim Generator
        ↓
CandidateClaim
        ↓
Claim Validation
```

---

## P0-07 UpgradeCandidate 的产生路径不闭合

005 接收：

```text
Upgrade Candidate
```

004 可以在确认 SKILL_GAP 后生成 Upgrade Candidate。

但有些 SKILL_GAP 在 003 已经足够明确，此时按 004 的规则“不应该继续 Experience Mining”。

于是可能出现：

```text
003 SKILL_GAP
→ 跳过 004
→ 005 需要 UpgradeCandidate
→ 谁创建？
```

### 建议

`UpgradeCandidate` 不由 004 独占。

改成：

```text
003
→ Gap + UpgradeFeasibility

004（如果需要 Mining）
→ Refined Gap

Workflow / 005
→ 根据 Gap 创建 UpgradeCandidate
```

005 应可以直接接收：

```text
Gap
+
UpgradeFeasibility
+
Target Requirement
```

并生成 Upgrade Candidate / Upgrade Assessment。

---

## P0-08 003 缺少 Broad / Targeted 两种 Match 深度

002 明确：

```text
Broad Apply
→ Lightweight Analysis / Match

Targeted Apply
→ Deep Analysis
```

004 又明确 Broad 默认不进入完整 Experience Mining。

但 003 当前没有明确 Broad / Targeted 两种执行深度，容易导致：

```text
所有 Broad Job
都执行完整 Requirement Extraction
+ Multi Experience Match
+ Gap Analysis
+ Upgrade Feasibility
```

破坏之前确定的成本策略。

### 建议

003 增加：

```text
BROAD_MATCH
```

只做：

```text
Lightweight Requirement Extraction
Structured Retrieval
Lightweight Match Summary
```

用于 Ranking / Promote Decision。

以及：

```text
TARGETED_MATCH
```

执行完整：

```text
Requirement Priority
Multi-Experience Match
Score + Status
Gap Analysis
Upgrade Feasibility
```

---

# 3. P1 — 强烈建议统一

## P1-01 Canonical Job 不应直接持有 Strategy

002 的 Canonical Job Model 当前包含：

```text
strategy
```

但：

```text
BROAD_APPLY / TARGETED_APPLY
```

不是岗位自身事实，而是“用户如何处理这个岗位”的决策。

007 Application 又保存 Strategy。

### 建议

分离：

```text
Job
= 外部岗位事实
```

```text
JobCandidate / JobDecision
= 当前用户对 Job 的求职决策
```

例如：

```text
JobCandidate
├── jobId
├── userId
├── ranking
├── filterResult
└── strategy
```

Application 创建时：

```text
Application.strategy
= strategy snapshot
```

避免修改 Strategy 后影响历史 Application。

---

## P1-02 Career Profile 与 Career Master Profile 容易形成双 Source of Truth

001：

```text
Career Profile
= 长期职业事实资产
```

006：

```text
Career Master Profile
= 完整职业资产集合
```

如果两者都被实现为独立 Aggregate，会出现：

```text
哪个才是 Source of Truth？
```

### 建议

保持：

```text
CareerProfile
= Core Career Fact Aggregate
```

而：

```text
CareerMasterProfile
= Logical Read Model / View
```

它组合：

```text
CareerProfile
+
Claims
+
Evidence
+
Interview Assets
```

不单独复制事实数据。

---

## P1-03 Resume Version 缺少明确生命周期状态

006 最终产生：

```text
Approved Resume Version
```

007 又依赖：

```text
Approved Role Resume
Approved Job-specific Resume
```

但 006 没有正式定义 ResumeVersion Status。

### 建议

增加：

```text
ResumeVersionStatus:
DRAFT
IN_REVIEW
APPROVED
REJECTED
SUPERSEDED
```

007 只允许：

```text
APPROVED
```

Resume Version 用于正式 Application。

---

## P1-04 Application Status 与 Outcome Event 需要明确关系

007 同时存在：

```text
ApplicationStatus:
VIEWED
REPLIED
INTERVIEWING
REJECTED
OFFER
```

以及独立：

```text
Outcome
```

容易出现重复 Source of Truth。

### 建议

定义：

```text
Outcome
= append-only event / observation
```

例如：

```text
VIEWED
REPLIED
INTERVIEW_INVITE
REJECTED
OFFER
```

而：

```text
Application.currentStatus
```

是根据最新有效 Outcome 派生出来的当前快照。

不要让两个模型各自独立修改。

---

## P1-05 Skill / Fact / Claim 的 Provenance 要使用不同类型名称

当前多个 Feature 都存在 `source / provenance`：

```text
Fact Provenance
Skill Source
Claim Provenance
Evidence Source
```

语义不同。

### 建议

避免统一成一个巨大 `SourceType`。

分别使用：

```text
FactProvenanceType
SkillSupportType
ClaimProvenanceType
EvidenceType / EvidenceSource
```

这样 AI 和代码都不容易混淆。

---

## P1-06 Match 应明确 Skill Depth 的证据优先级

001 定义：

```text
Self-assessed Depth
≠
Verified Depth
```

003 负责 Match，但还应更明确使用规则。

### 建议

Match 判断优先级：

```text
Verified Depth
>
Experience-supported Depth
>
Self-assessed Depth
```

Self-assessed Depth 可以作为 Retrieval / Question Generation 信号，但不能单独把 Requirement 判为 MATCHED。

---

## P1-07 所有 Score 必须名字空间化

当前有：

```text
Job Ranking Score
Retrieval Candidate Score
Requirement Match Score
```

如果代码里都叫：

```text
score
```

很容易混淆。

### 建议

统一概念名：

```text
jobRankingScore
retrievalRelevanceScore
requirementMatchScore
jobMatchSummaryScore
```

Spec 中也尽量使用完整名称。

---

# 4. P2 — 可以保留，但应在术语表统一

以下差异本身没有问题：

```text
UNKNOWN
FAILED
CANCELLED
APPROVED
```

可以在不同 Domain 中重复出现，只要使用完整类型：

```text
HardFilterResult.UNKNOWN
MatchStatus.UNKNOWN

CareerWorkflowStatus.FAILED
ProjectUpgradeStatus.FAILED
```

不要使用一个全局 Enum 承载所有状态。

---

# 5. 推荐的跨 Feature Source of Truth

建议最终固定：

| 领域 | Source of Truth |
|---|---|
| 用户职业事实 | CareerProfile / Career Fact |
| Job 原始事实 | Job |
| 用户对 Job 的处理策略 | JobCandidate / JobDecision |
| Requirement | Requirement |
| Match | RequirementMatch |
| Gap | GapAnalysis |
| Experience Mining 结果 | ExperienceAsset |
| Upgrade 执行 | ProjectUpgrade |
| 工程验证证据 | EvidenceArtifact |
| 对外陈述 | Claim |
| 简历 | ResumeVersion |
| 投递 | Application |
| 投递反馈 | Outcome |
| 长流程执行 | CareerWorkflow |
| 人工决策 | HumanDecision / HumanApproval |

---

# 6. 推荐的 Feature Handoff

统一后建议：

```text
001
CareerProfile
Experience
Project
Skill
Capability
CareerWorkflow
HumanDecision Foundation
        ↓
002
Job
JobCandidate
JobRanking
        ↓
003
Requirement
RequirementMatch
GapAnalysis
UpgradeFeasibility
        ↓
004
ExperienceAsset
Refined Facts
ClaimSuggestion
EvidenceRequirement
        ↓
005
ProjectUpgrade
Validated Capability
EvidenceArtifact
        ↓
006
CandidateClaim
ClaimPool
ResumeVersion
InterviewStory
        ↓
007
Application
Outcome
Observation
Hypothesis
Recommendation
```

---

# 7. 建议修订顺序

建议不要一次大改所有文字。

按依赖顺序：

```text
Step 1
修 001
- Status 分层原则
- HumanDecision Foundation
- CareerProfile / Master Profile 边界

Step 2
修 002
- Job 与 JobCandidate / Strategy 分离

Step 3
修 003
- 删除 UPGRADABLE MatchStatus
- 修 Gap 历史条件
- 增加 Broad / Targeted Match Mode

Step 4
修 004
- Candidate Claims → ClaimSuggestion
- UpgradeCandidate Ownership
- Evidence Gap Routing

Step 5
修 005
- WorkflowStatus 与 UpgradeStatus 分离
- Evidence Build 职责

Step 6
修 006
- CandidateClaim 唯一所有权
- ResumeVersionStatus
- CareerMasterProfile = Read Model

Step 7
修 007
- WorkflowStatus 与 ApplicationStatus 分离
- Outcome Event / currentStatus 关系
- Strategy Snapshot
```

---

# 8. 结论

目前 7 个 Feature Spec 的业务设计方向是一致的：

```text
真实职业事实
→ 真实岗位
→ 可解释匹配
→ 真实经历澄清
→ 真实能力升级
→ 可验证 Claim
→ 高质量 Resume
→ 安全投递
→ Outcome Feedback
```

主要问题不是产品方向，而是部分跨 Feature 概念在不同文档中被重复定义。

在进入 `$speckit-plan` 前完成上述 P0 修订，可以显著减少：

- 重复实体；
- 双状态机；
- Agent 自行决定流程；
- Gap 无下游；
- Claim 所有权冲突；
- Application 重复副作用；
- AI Coding Agent 对 Spec 的不同理解。
