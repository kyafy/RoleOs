# 006 — Claim-Evidence & Resume

> **Feature ID**：006  
> **Feature Name**：Claim-Evidence & Resume  
> **Product**：RoleOS  
> **Stage**：V1  
> **Status**：Revised after Cross-Feature Consistency Review

---

# 1. 目标

Claim-Evidence & Resume 是 RoleOS V1 中负责“确认能说什么，以及如何把真实能力表达出来”的能力。

它承接前面已经形成的：

```text
Career Fact
+
Experience
+
Project
+
Verified / Supported Capability
+
Evidence
```

并将其转换为：

```text
Candidate Claim
→ Claim Validation
→ Claim Pool
→ Resume Compilation
→ Interview Story
```

核心原则：

> **先确认能说什么，再决定怎么说。**

同时必须遵守：

```text
Truth
>
Relevance
>
Keyword Coverage
```

但真实性约束不意味着保守表达。

RoleOS 应做到：

> **在真实性边界内，最大化表达质量。**

---

# 2. 范围

006 负责：

- 接收 004 的 ClaimSuggestion；
- Candidate Claim 生成与唯一生命周期所有权；
- Claim Provenance；
- Claim Status；
- Evidence Intake / Validation / Claim-Evidence Trace；
- Historical Fact / Derived Capability / Upgraded Capability / Designed Scenario 区分；
- Unsupported Claim 拦截；
- Quantitative Claim Validation；
- Claim Pool；
- Career Master Profile；
- Role Resume；
- Job-specific Resume；
- Resume Compiler；
- JD Coverage；
- Resume Version；
- Interview Story；
- Claim Review；
- Resume Preview；
- Human Review。

本 Feature 不负责：

- 新 Job Discovery；
- Experience Mining；
- Project Upgrade 执行；
- 新 Test / Eval / Trace / Benchmark / Demo 等工程 Evidence 的构建（由 005 负责）；
- Application Submit；
- Outcome Feedback。

---

# 3. 核心 User Stories

## US-006-01 — 确认哪些内容可以说

作为用户，我希望 RoleOS 能根据 Career Fact、Experience 和 Evidence 判断哪些 Claim 可以安全进入简历。

## US-006-02 — 统一表达新旧能力

作为用户，我希望通过 Project Upgrade 新获得的真实能力能够与已有真实能力以一致质量呈现在简历中，而不是被标记成“后补技能”或低一档能力。

## US-006-03 — 生成岗位相关简历

作为用户，我希望 RoleOS 能根据 Role / Job 选择、排序和重写已有 Claim，生成更适合当前岗位的 Resume。

## US-006-04 — 可追溯 Claim

作为用户，我希望重要 Claim 可以反向追溯到 Experience / Project / Evidence，从而支持 Resume Review 和 Interview。

---

# 4. Claim Model

Claim 表示：

> **RoleOS 准备对外表达的一条职业陈述。**

例如：

```text
基于 LangGraph 构建支持 Checkpoint 与 Human-in-the-loop 的 Agent Workflow。
```

Claim 不是原始 Fact。

004 可以输出 `ClaimSuggestion`，但正式 `CandidateClaim / ClaimStatus / ClaimPool` 只由 006 创建和维护。

它是：

```text
Career Fact
+
Context
+
Evidence
+
Target Relevance
→
External Expression
```

---

# 5. Claim Provenance

V1 至少区分：

```text
HISTORICAL_FACT
DERIVED_CAPABILITY
UPGRADED_CAPABILITY
DESIGNED_SCENARIO
```

---

## 5.1 HISTORICAL_FACT

过去真实发生过的工作、项目或职责事实。

例如：

```text
在某项目中负责商品发布流程设计。
```

---

## 5.2 DERIVED_CAPABILITY

由已确认 Experience / Project 推导出的真实能力。

例如：

```text
多个项目共同证明用户具备 RAG Engineering 能力。
```

---

## 5.3 UPGRADED_CAPABILITY

通过 005 Project Upgrade 新增并经过 Validation 支撑的能力。

例如：

```text
MCP Server
Hybrid Retrieval
Rerank
Evaluation
```

---

## 5.4 DESIGNED_SCENARIO

为了展示能力而设计的真实项目场景。

例如：

```text
为展示 Agent Workflow Recovery 设计新的 Portfolio Scenario。
```

它可以是真实项目，但不能冒充历史公司业务。

---

# 6. Provenance 与最终表达

Claim Provenance 是内部真实性机制。

它不应该自动变成简历上的能力等级。

必须保持：

```text
Internal Provenance
≠
External Skill Tier
```

例如：

```text
Java
LangGraph
MCP
RAG
```

可以在 Skills 中统一呈现。

不能因为 MCP 来源于 Project Upgrade，就强制写成：

```text
MCP（新增）
MCP（后补）
MCP（Portfolio）
```

---

## 6.1 Presentation Parity

如果能力已经真实成立并满足 Claim 条件：

```text
HISTORICAL_FACT
DERIVED_CAPABILITY
UPGRADED_CAPABILITY
```

在简历文案质量上：

```text
没有默认高低之分
```

它们都可以：

- 正常进入 Skills；
- 正常进入 Project Description；
- 正常用于 Job Match；
- 正常用于 Interview Story。

---

## 6.2 Temporal Truth

虽然表达质量保持一致，但时间事实不能篡改。

必须保持：

```text
Current Verified Capability
≠
Historical Work Fact
```

例如：

```text
2024 公司项目没有 MCP
2026 Project Upgrade 新增 MCP
```

可以表达：

```text
当前具备 MCP 实践能力
```

但不能写成：

```text
2024 年任职期间已经使用 MCP
```

因此 Provenance 会影响：

```text
Claim Eligibility
Temporal Placement
Evidence Trace
```

而不是自动降低文案质量。

---

# 7. Claim Status

V1 使用四档：

```text
VERIFIED
SUPPORTED
NEEDS_CONFIRMATION
UNSUPPORTED
```

---

## 7.1 VERIFIED

存在足够直接 Evidence。

例如：

```text
Source Code
Test
Evaluation
Git Commit
Trace
```

能够直接支撑 Claim。

---

## 7.2 SUPPORTED

有可信 Career Fact / Experience / Project 支撑，但 Evidence 强度较弱。

SUPPORTED 可以正常进入最终 Resume。

---

## 7.3 NEEDS_CONFIRMATION

Claim 中包含：

- Agent 新提取的事实；
- 用户尚未确认的结构化信息；
- 仍存在真实性不确定的信息。

不能进入最终 Resume。

---

## 7.4 UNSUPPORTED

没有足够事实依据或 Evidence。

不得进入最终 Resume。

---

# 8. Confirmation 与 Status Transition

用户确认 Claim 背后的事实后：

```text
NEEDS_CONFIRMATION
→
SUPPORTED or VERIFIED
```

具体状态取决于 Evidence。

### 只有用户确认

```text
NEEDS_CONFIRMATION
→ SUPPORTED
```

### 用户确认 + 足够 Evidence

```text
NEEDS_CONFIRMATION
→ VERIFIED
```

核心规则：

> **User Confirmation ≠ Verification。**

---

# 9. Resume Eligibility

最终 Resume 只允许使用：

```text
VERIFIED
SUPPORTED
```

以下状态不得进入最终投递版本：

```text
NEEDS_CONFIRMATION
UNSUPPORTED
```

---

## 9.1 VERIFIED 与 SUPPORTED 的外部表达

必须保持：

> **VERIFIED 和 SUPPORTED 使用相同文案质量。**

简历中不显示：

```text
[VERIFIED]
[SUPPORTED]
```

状态差异只存在于 RoleOS 内部，用于：

- 可信度；
- Trace；
- Future Validation；
- Interview Preparation。

---

# 10. Quantitative Claim

任何量化结果必须有真实依据。

允许的来源：

```text
Measured Metric
User-confirmed Historical Number
Verified Evaluation Result
```

禁止：

```text
Agent Estimated Number
Unmeasured Improvement
Fabricated Percentage
```

---

# 11. 什么是真实结果

真实结果不要求必须来自 Production。

定义：

> **在真实可执行系统上，通过可复现的测量过程实际观察得到的结果。**

例如：

```text
JMeter
100 concurrency
same environment
before / after
```

测得：

```text
P95 Latency
1.2s → 760ms
```

属于真实结果。

可以写：

```text
在 JMeter 100 并发基准测试中将 P95 延迟由 1.2s 降至 760ms。
```

但如果不是 Production：

```text
不得写成“生产环境 P95 延迟降低”
```

---

## 11.1 Claim Scope Rule

必须保持：

```text
Claim Scope
<=
Evidence Scope
```

例如：

```text
离线评测集 Recall@5
0.61 → 0.74
```

可以写：

```text
在离线评测集上将 Recall@5 从 0.61 提升至 0.74。
```

不能直接扩张为：

```text
线上业务召回率提升 21%
```

---

## 11.2 Quantitative Evidence

量化 Claim 至少应记录：

```text
Metric
Baseline
After
Test Condition
Evidence
```

例如：

```text
Metric:
P95 Latency

Baseline:
1.2s

After:
760ms

Condition:
JMeter / 100 concurrency / same environment

Evidence:
.jmx
HTML Report
Raw Result
Git Commit
```

---

# 12. Career Master Profile

Career Master Profile 是：

> **RoleOS 面向 Claim / Resume 的职业资产聚合视图（Logical Read Model）。**

它不是直接投递给招聘方的 Resume，也不是第二套 Career Fact Source of Truth。

逻辑上组合：

```text
CareerProfile / Career Facts
+
Claims
+
Evidence
+
Interview Assets
```

必须保持：

```text
CareerProfile / Career Fact = 事实 Source of Truth
CareerMasterProfile = Read Model / Aggregated View
```

CareerMasterProfile 不应复制后独立维护 Experiences / Projects / Skills / Capabilities 的事实版本。

---

# 13. Claim Pool

Claim Pool 保存所有：

```text
VERIFIED
SUPPORTED
```

且允许对外表达的 Claim。

Resume Compiler 不直接从原始 Career Fact 临时编故事。

默认链路：

```text
Career Master Profile
→ Claim Pool
→ Resume Compiler
```

---

# 14. Resume

Resume 是：

> **RoleOS 根据某个求职目标，从 Claim Pool 中选择、排序、压缩和表达出来的求职版本。**

必须保持：

```text
Resume
≠
Source of Truth
```

长期事实 Source of Truth 仍然是 `CareerProfile / Career Fact`；Career Master Profile 只是供 Claim / Resume 使用的聚合视图。

---

# 15. Resume Layer

V1 使用：

```text
Career Master Profile
        ↓
Claim Pool
        ↓
Role Resume
        ↓
Job-specific Resume
```

---

## 15.1 Role Resume

针对某一类 Target Role 的稳定 Resume。

例如：

```text
AI Agent Engineer
```

主要用于：

```text
BROAD_APPLY
```

避免为大量相似岗位重复生成大量简历。

---

## 15.2 Job-specific Resume

针对一个具体 Target Job。

主要用于：

```text
TARGETED_APPLY
```

允许：

```text
重新排序
裁剪
压缩
关键词对齐
项目选择
Claim 强调
```

但不允许：

```text
新增事实
修改项目历史
改变发生时间
把 Upgrade 倒灌为历史事实
虚构指标
```

---

# 16. Resume Compiler

Resume Compiler 输入：

```text
Target Job
JD Requirements
Requirement Priority
Claim Pool
Relevant Experience
Skills / Capabilities
Evidence
```

输出：

```text
Resume Version
```

核心目标：

```text
Truth
→ Relevance
→ Coverage
→ Clarity
```

---

# 17. JD Coverage

Resume Compiler 应检查：

```text
MUST_HAVE
IMPORTANT
NICE_TO_HAVE
```

哪些已经被 Resume 覆盖。

Coverage 不是为了强行塞关键词。

如果某 Requirement 当前没有真实能力支撑：

```text
不能为了 Coverage 生成 Unsupported Claim
```

---

# 18. Evidence Intake & Claim-Evidence Trace

006 可以接收两类 Evidence：

```text
005 产生的 EvidenceArtifact
+
用户已有的 Historical / Existing Evidence
```

对于用户已有材料，006 负责：

```text
Intake
→ Source / Scope Check
→ Link to Fact / Experience / Project
→ Claim Eligibility Review
```

006 不通过修改代码去制造新的 Test / Eval / Trace；需要新工程 Evidence 时，应创建 Evidence Requirement 并路由到 005。

重要 Claim 应至少能追溯到：

```text
Claim
→ Experience / Project
→ Fact
→ Evidence
```

例如：

```text
Claim:
基于 Hybrid Retrieval + Rerank 提升检索效果

→ Project Upgrade
→ Evaluation Result
→ Git Commit
```

对于 SUPPORTED Claim：

```text
至少能追溯到可信 Career Fact / Experience
```

---

# 19. Interview Story

006 同时生成重要 Claim 对应的 Interview Story。

不为所有 Claim 生成长故事。

优先：

```text
MUST_HAVE Requirement
IMPORTANT Requirement
Targeted Job
High-value Claim
```

Story 结构：

```text
Context
Business Problem
Challenge
Decision
Implementation
Evaluation
Trade-off
Retrospective
```

Interview Story 和 Resume Claim 必须来自同一个事实底座。

---

# 20. Resume Version

每次生成或修改 Resume，都应形成明确版本。

V1 使用：

```text
ResumeVersionStatus:
DRAFT
IN_REVIEW
APPROVED
REJECTED
SUPERSEDED
```

至少保留：

```text
Target Role / Job
Source Claims
Generated Time
Status
```

只有：

```text
ResumeVersionStatus = APPROVED
```

的 Resume Version 才允许被 Feature 007 用于正式 Application。

新版本被批准后，旧版本可以进入：

```text
SUPERSEDED
```

但历史 Application 继续引用当时实际使用的旧版本，不被覆盖。

允许后续追踪：

```text
哪个 Job
用了哪份 Resume
最终 Outcome 是什么
```

供 Feature 007 使用。

---

# 21. Human Review

最终 Resume 必须经过用户 Review。

该决策属于 `ResumeReviewDecision`，通过 001 的通用 `HumanDecision` 机制承载，但不要求与 Project Upgrade / Application 共用同一个业务枚举。

支持：

```text
ACCEPT
EDIT
REJECT
REQUEST_REWRITE
```

典型状态变化：

```text
DRAFT
→ IN_REVIEW
→ ACCEPT → APPROVED
→ REJECT → REJECTED

EDIT / REQUEST_REWRITE
→ 产生新的 DRAFT / IN_REVIEW Version
```

---

## 21.1 用户 Edit

如果用户只是修改表达：

```text
不改变事实
```

可以直接更新 Narrative。

如果用户新增了新的事实：

```text
必须重新进入 Fact / Claim Confirmation
```

不能因为是在 Resume Editor 中输入，就自动成为 VERIFIED Claim。

---

# 22. Web UI

006 至少包含：

```text
Claim Review
+
Resume Preview
```

---

## 22.1 Claim Review

显示：

```text
Claim
Status
Provenance
Supporting Experience
Evidence
Target Requirement
Warning
```

用户可以：

```text
Confirm
Edit
Reject
Request Evidence
```

---

## 22.2 Resume Preview

显示：

```text
Resume Version
Target Role / Job
JD Coverage
Selected Claims
Resume Content
Review Status
```

用户可以：

```text
Accept
Edit
Reject
Request Rewrite
```

---

## 22.3 Trace View

对于重要 Claim，用户可以查看：

```text
Claim
↓
Source Experience / Project
↓
Evidence
↓
Validation Result
```

但这些内部 Trace 默认不写入最终简历。

---

# 23. Core Business Rules

```text
R-001
Claim ≠ Fact
```

Claim 是基于真实事实生成的外部表达。

```text
R-002
Internal Provenance ≠ External Skill Tier
```

来源不自动决定简历能力等级。

```text
R-003
Upgraded Capability receives presentation parity
```

真实新增能力与已有能力在文案质量上没有默认高低之分。

```text
R-004
Temporal Truth must be preserved
```

后来新增的能力不能倒灌成过去任职事实。

```text
R-005
User Confirmation ≠ Verification
```

确认只能解决真实性确认，不能自动创造 Evidence。

```text
R-006
Only VERIFIED / SUPPORTED enter final Resume
```

NEEDS_CONFIRMATION / UNSUPPORTED 不得进入。

```text
R-007
VERIFIED and SUPPORTED have equal presentation quality
```

状态差异不直接暴露给招聘方。

```text
R-008
Claim Scope <= Evidence Scope
```

表达范围不能超过证据范围。

```text
R-009
Measured Result ≠ Production Result
```

如果不是 Production，Claim 必须保留测试场景边界。

```text
R-010
Resume ≠ Source of Truth
```

Resume 是 Career Asset 的编译产物。

```text
R-011
Resume may optimize presentation, not facts
```

可以重排和重写，不能新增事实。

```text
R-012
Important Claim must be traceable
```

必须能够追溯到 Experience / Project / Evidence。

```text
R-013
006 owns CandidateClaim lifecycle
```

004 只提供 ClaimSuggestion，不创建正式 CandidateClaim。

```text
R-014
CareerMasterProfile is a Read Model
```

Career Fact 的 Source of Truth 始终在 CareerProfile / Career Fact。

---

# 24. Acceptance Scenarios

## AC-001 — Upgraded Skill Presentation

Given：

```text
用户原本不会 MCP
005 完成 MCP Upgrade
Validation PASS
```

Then：

```text
MCP 可以正常进入 Skills / Project Claim
```

最终 Resume 不强制显示：

```text
MCP（新增）
MCP（Upgrade）
```

---

## AC-002 — Temporal Truth

Given：

```text
2024 公司项目无 MCP
2026 Project Upgrade 增加 MCP
```

Then：

```text
当前 MCP Capability 可以进入 Resume
```

但不得将 MCP Claim 放入：

```text
2024 任职期间的历史事实
```

造成时间误导。

---

## AC-003 — Needs Confirmation

Given：

```text
Agent 生成 Candidate Claim
Status = NEEDS_CONFIRMATION
```

When：

```text
用户确认事实
```

Then：

如果没有直接 Evidence：

```text
Status → SUPPORTED
```

如果 Evidence 足够：

```text
Status → VERIFIED
```

---

## AC-004 — Supported Resume Claim

Given：

```text
Claim = SUPPORTED
```

Then：

```text
允许进入最终 Resume
```

并使用与 VERIFIED 相同的文案质量。

---

## AC-005 — Unsupported Claim

Given：

```text
没有 Career Fact
没有 Experience
没有 Evidence
```

Then：

```text
Claim = UNSUPPORTED
```

不得进入最终 Resume。

---

## AC-006 — JMeter Result

Given：

```text
同环境
JMeter 100 concurrency
Before P95 = 1.2s
After P95 = 760ms
```

Then：

允许生成：

```text
在 JMeter 100 并发基准测试中将 P95 延迟由 1.2s 降至 760ms。
```

不得自动改写为：

```text
生产环境性能提升 37%
```

---

## AC-007 — RAG Evaluation

Given：

```text
离线 Evaluation Dataset
Recall@5:
0.61 → 0.74
```

Then：

允许：

```text
在离线评测集上将 Recall@5 从 0.61 提升至 0.74。
```

---

## AC-008 — Role Resume

Given：

```text
Target Role = AI Agent Engineer
Workflow = BROAD_APPLY
```

Then：

```text
优先使用 Role Resume
```

---

## AC-009 — Job-specific Resume

Given：

```text
Workflow = TARGETED_APPLY
Specific Job exists
```

Then：

Resume Compiler 可以：

```text
重新排序
裁剪
关键词对齐
项目选择
Claim 强调
```

但不得改变事实。

---

## AC-010 — Claim Trace

用户点击一个重要 Resume Claim：

```text
可以追溯到
Experience / Project / Evidence
```

---

## AC-011 — Interview Story

对于 Targeted Job 的重要 Claim：

```text
能够生成结构化 Interview Story
```

且 Story 与 Resume Claim 使用同一事实底座。

---

# 25. Edge Cases

### User Edits Resume With New Fact

如果用户在 Resume Editor 中新增：

```text
“负责 10 人团队”
```

但 Career Profile 中没有该事实：

```text
不能直接保存为最终 Claim
```

必须进入确认流程。

---

### Evidence Lost

如果某 VERIFIED Claim 的 Evidence 后续不可访问：

```text
不得静默保持 VERIFIED
```

应进入重新验证或降级流程。

V1 可以先标记：

```text
Evidence Unavailable
```

---

### Conflicting Claims

如果两个 Claim 对同一事实表达冲突：

```text
不得同时进入最终 Resume
```

需要 Review。

---

### Too Many Keywords

JD Keyword Coverage 不得驱动：

```text
关键词堆砌
```

如果 Skill 没有真实支撑：

```text
不进入 Resume
```

---

### Designed Scenario

Designed Scenario 可以作为真实项目表达。

但不能：

```text
伪装成真实企业客户项目
```

---

# 26. Definition of Done

006 Claim-Evidence & Resume 完成必须满足：

- [ ] 004 的 ClaimSuggestion 只能作为输入，006 唯一拥有 CandidateClaim 生命周期；
- [ ] Claim 区分 HISTORICAL_FACT / DERIVED_CAPABILITY / UPGRADED_CAPABILITY / DESIGNED_SCENARIO；
- [ ] Provenance 不直接映射成外部能力等级；
- [ ] Upgraded Capability 与已有能力保持一致表达质量；
- [ ] Temporal Truth 受到保护；
- [ ] Claim Status 支持 VERIFIED / SUPPORTED / NEEDS_CONFIRMATION / UNSUPPORTED；
- [ ] 用户确认后根据 Evidence 转为 SUPPORTED 或 VERIFIED；
- [ ] Final Resume 只允许 VERIFIED / SUPPORTED Claim；
- [ ] VERIFIED / SUPPORTED 不在简历中展示内部标签；
- [ ] 量化 Claim 必须有真实测量依据；
- [ ] Claim Scope 不超过 Evidence Scope；
- [ ] JMeter / Eval 等真实基准结果可以进入 Claim；
- [ ] Career Master Profile 是聚合 Read Model，不形成第二套 Career Fact Source of Truth；
- [ ] Career Master Profile 与 Resume 明确区分；
- [ ] Claim Pool 成为 Resume Compiler 的主要输入；
- [ ] Broad Apply 使用 Role Resume；
- [ ] Targeted Apply 支持 Job-specific Resume；
- [ ] Resume 可以重排、裁剪、压缩和关键词对齐；
- [ ] Resume 不得新增 Career Fact；
- [ ] 重要 Claim 可追溯到 Experience / Project / Evidence；
- [ ] 006 支持已有 Evidence Intake / Validation / Claim Linking；
- [ ] 新工程 Evidence Requirement 会路由到 005；
- [ ] 支持 Interview Story；
- [ ] ResumeVersionStatus 支持 DRAFT / IN_REVIEW / APPROVED / REJECTED / SUPERSEDED；
- [ ] 只有 APPROVED Resume Version 可以进入正式 Application；
- [ ] Web UI 支持 Claim Review / Resume Preview / Trace View；
- [ ] Human Review 支持 ACCEPT / EDIT / REJECT / REQUEST_REWRITE；
- [ ] Acceptance Scenarios 通过；
- [ ] 未提前实现 Application / Outcome Feedback。

最终验收 Demo：

```text
Career Master Profile
+
Experience
+
Project Upgrade Result
+
Evidence
        ↓
CandidateClaim(s)
        ↓
VERIFIED / SUPPORTED / NEEDS_CONFIRMATION / UNSUPPORTED
        ↓
Claim Review
        ↓
Claim Pool
        ↓
Target Job / Role
        ↓
Resume Compiler
        ↓
Role Resume / Job-specific Resume
        ↓
JD Coverage
        ↓
Resume Preview
        ↓
Human Review
        ↓
Approved Resume Version
```

如果 RoleOS 能够稳定回答：

```text
这句话为什么可以写？
它来自什么事实？
有什么 Evidence？
可以写到什么程度？
如何针对当前 Job 把它表达得更有竞争力？
```

则：

> **006 Claim-Evidence & Resume 验收通过。**
