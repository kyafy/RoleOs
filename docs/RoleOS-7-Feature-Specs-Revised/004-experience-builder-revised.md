# 004 — Experience Builder

> **Feature ID**：004  
> **Feature Name**：Experience Builder  
> **Product**：RoleOS  
> **Stage**：V1  
> **Status**：Revised after Cross-Feature Consistency Review

---

# 1. 目标

Experience Builder 是 RoleOS V1 中负责“把没有讲清楚的真实经历挖出来”的能力。

它不创造经历，也不替用户补技能。

它主要解决：

```text
用户其实做过，但 Career Profile 没记录清楚什么？
哪些 UNKNOWN / PARTIAL_MATCH 实际只是信息缺失？
哪些问题值得继续追问？
哪些 Gap 应该转交 Project Upgrade、Technical Evidence Build 或 Evidence Intake？
```

核心流程：

```text
Target Job / Requirement
→ Current Match / Gap
→ Decide Whether to Mine
→ Generate High-value Questions
→ User Answer
→ Fact Candidate
→ User Confirmation
→ Update Experience
→ Re-evaluate Match / Gap
→ Stop or Continue
```

核心原则：

> **Experience Mining 负责发现真实事实，不负责创造事实。**

---

# 2. 范围

004 负责：

- 判断是否需要进入 Experience Mining；
- 基于 Target Job / Requirement / Gap 动态追问；
- 每轮提出少量高价值问题；
- 避免重复提问；
- 将用户回答结构化为 Fact Candidate；
- 用户确认 Agent 产生的结构化事实；
- 更新 Experience / Project；
- 生成 Experience Story；
- 重新判断 Gap；
- 输出统一 Experience Asset；
- 输出 Claim Suggestion / Upgrade Signal / Evidence Requirement，供后续 Feature 消费；
- Experience Builder Web 工作区。

本 Feature 不负责：

- 新技能真实学习；
- Project Upgrade 执行；
- Codex 调用；
- Evidence 验证；
- Resume Compiler；
- Application；
- 虚构生产经历；
- 将 Designed Scenario 写成真实工作经历。

---

# 3. 核心 User Stories

## US-004-01 — 补全真实经历

作为用户，我希望 RoleOS 针对当前岗位要求追问我最有价值的问题，把我确实做过但没有写清楚的经历补全。

## US-004-02 — 减少无效提问

作为用户，我希望系统不要用固定问卷把所有问题都问一遍，而是只围绕当前 Requirement / Gap 提问，并避免重复询问已经确认的信息。

## US-004-03 — 区分“没说清楚”和“真的不会”

作为用户，我希望系统通过追问判断当前 Gap 到底属于 Story Gap、Skill Gap 还是 Evidence Gap，以便后续进入正确流程。

## US-004-04 — 形成可复用 Experience Asset

作为用户，我希望一次 Experience Mining 的结果能够沉淀为长期 Career Asset，后续其他 Job、Resume 和 Interview 都可以复用。

---

# 4. Experience Mining 在业务流中的角色

Experience Mining 位于：

```text
JD Requirement
      ↓
Experience Match
      ↓
Gap
      ↓
Experience Mining
      ↓
Updated Career Facts
      ↓
Re-match
```

它的核心作用是：

> **降低“不知道用户有没有做过”带来的不确定性。**

典型分流：

```text
STORY_GAP
→ Experience Mining

SKILL_GAP
→ 005 Project Upgrade Assessment

EVIDENCE_GAP
→ Evidence Requirement → 005 Technical Evidence Build / 006 Evidence Intake
```

但在某些 UNKNOWN / PARTIAL_MATCH 场景中，也需要先通过 Experience Mining 判断真实情况。

---

# 5. 何时进入 Experience Mining

V1 必须明确 Experience Mining 的进入标准。

不使用复杂机器学习评分。

主要根据：

```text
Strategy
+
Gap Type
+
Requirement Priority
+
Information Sufficiency
+
Previous Questions
```

进行路由。

## 5.1 默认进入条件

满足以下任一情况时，可以进入 Experience Mining：

### A. STORY_GAP

```text
用户可能已经做过
但 Experience 表达不足
```

默认进入 Experience Mining。

### B. UNKNOWN 且原因是信息不足

例如：

```text
Requirement:
Evaluation

Current Experience:
只写了“优化了 RAG 效果”

系统无法判断：
是否真正做过 Evaluation
```

此时应先问用户，而不是直接判断 SKILL_GAP。

### C. PARTIAL_MATCH 且补充信息可能改变判断

例如：

```text
当前只确认做过 Tool Calling
但不知道是否处理过 Retry / Recovery
```

如果一个高价值问题可能让：

```text
PARTIAL_MATCH
→ MATCHED
```

则值得进入 Experience Mining。

## 5.2 Requirement Priority

Experience Mining 优先服务：

```text
MUST_HAVE
IMPORTANT
```

对于：

```text
NICE_TO_HAVE
OPTIONAL
```

默认不投入过多追问成本。

除非：

- 用户主动要求深挖；
- 该 Requirement 对 Targeted Apply 很有价值；
- 一个非常低成本的问题即可明确判断。

## 5.3 Strategy

### BROAD_APPLY

默认：

```text
不进入完整 Experience Mining
```

因为 Broad Apply 的目标是低成本覆盖。

允许：

```text
极少量必要澄清
```

但不进入完整 Experience Builder Workflow。

### TARGETED_APPLY

根据 Requirement / Gap 决定是否进入 Experience Mining。

Targeted Apply 是 Experience Builder 的主要使用场景。

## 5.4 不进入 Experience Mining 的场景

### 已确认 SKILL_GAP

如果用户已经明确：

```text
我没有做过 MCP
```

则不需要继续问。

直接进入：

```text
005 Upgrade Assessment
```

正式 `UpgradeCandidate` 由 005 创建。

### 纯 EVIDENCE_GAP

如果事实已经确认，只是缺证据：

```text
不需要继续问经历
```

应输出：

```text
Evidence Requirement
```

并根据缺口类型路由到 005 Technical Evidence Build 或 006 Evidence Intake。

### NOT_APPLICABLE Gap

例如：

```text
真实管理年限
真实企业客户经验
真实生产规模
```

不能靠 Experience Mining 或 Project Upgrade 消除。

### 已经信息充分

如果当前事实已经足够支撑 Match 判断：

```text
不继续为了“故事更完整”无限追问
```

---

# 6. Question Generation

Experience Builder 不采用固定问卷。

问题必须由：

```text
Target Job
+
Requirement
+
Current Experience
+
Current Gap
+
Known Facts
```

动态生成。

## 6.1 每轮问题数量

每轮：

```text
1～3 个高价值问题
```

避免一次向用户抛出大量问题。

## 6.2 Question Priority

问题分三档：

```text
P0
P1
P2
```

### P0

不回答就无法判断当前 Requirement。

### P1

答案会明显影响：

```text
Match
Claim
Gap Type
Upgrade Decision
```

### P2

只会让 Story 更丰富，但不会明显改变业务判断。

默认优先：

```text
P0
→ P1
→ P2
```

P2 不应成为长流程的主要驱动力。

## 6.3 Information Gain 原则

系统应优先选择：

> **最可能改变当前 Match / Gap 判断的问题。**

例如：

```text
Requirement:
Evaluation
```

优先问：

```text
是否定义过评测集？
使用了什么指标？
是否比较过不同方案？
```

而不是优先问与当前 Requirement 无关的问题。

---

# 7. Experience Mining Focus

追问可以围绕：

```text
Context
Problem
Goal
Responsibility
Decision
Implementation
Trade-off
Result
Evaluation
Retrospective
```

但这不是固定问卷。

只有当前 Requirement / Gap 需要时才追问对应信息。

---

# 8. Fact Candidate & Confirmation

## 8.1 用户原始回答

用户直接输入：

```text
USER_INPUT
```

本身不需要再次确认。

## 8.2 Agent 结构化结果

Agent 将用户回答转成新的结构化 Career Fact 时：

```text
User Answer
→ Agent Structuring
→ Fact Candidate
→ User Confirmation
→ Career Fact
```

必须沿用 001 的真实性规则。

## 8.3 禁止自动扩张事实

例如用户说：

```text
“用了 LangGraph 的 checkpointer”
```

Agent 可以生成：

```text
使用 LangGraph Checkpointer 持久化 Workflow State
```

但不能自动扩写为：

```text
设计了企业级高可用 Agent Recovery Architecture
```

除非用户提供了相应真实事实。

---

# 9. Experience Story

Experience Builder 可以基于已经确认的事实生成 Narrative。

必须区分：

```text
Fact
≠
Narrative
```

如果只是表达优化，没有新增事实：

```text
不要求逐句事实确认
```

用户可以在最终 Story Preview 中整体：

```text
Accept
Edit
Request Rewrite
```

Narrative 不得反向覆盖 Career Fact。

---

# 10. Gap Routing

Experience Builder 完成一轮补充后，应重新判断当前 Gap。

## 10.1 STORY_GAP

004 的主要职责。

如果用户提供的信息足够：

```text
STORY_GAP
→ RESOLVED
```

## 10.2 SKILL_GAP

004 不负责补技能，也不拥有正式 `UpgradeCandidate` 生命周期。

如果确认：

```text
用户确实没有该能力
```

004 输出：

```text
Refined SKILL_GAP
+
Upgrade Signal
```

随后由 005 基于：

```text
Target Requirement
Gap
Upgrade Feasibility
Relevant Project Context
```

创建并评估正式 `UpgradeCandidate`。

## 10.3 EVIDENCE_GAP

004 只负责记录：

```text
Evidence Requirement
```

不直接构建或验证 Evidence。

后续根据缺口类型路由：

```text
需要新 Test / Eval / Trace / Benchmark / Demo
→ 005 Technical Evidence Build

用户已经存在历史材料，需要接入、验证或关联 Claim
→ 006 Evidence Intake & Claim Review
```

## 10.4 UNKNOWN

如果信息仍不足：

```text
继续问
```

如果继续询问的价值已经很低：

```text
保留 UNKNOWN
```

不能强制做结论。

---

# 11. Stop Condition

Experience Mining 不能无限追问。

结束判断由：

```text
Agent Assessment
+
Deterministic Rules
```

共同决定。

Agent 可以判断：

```text
当前信息是否已经足够
```

但 Agent 不能单独决定 Workflow 已完成。

## 11.1 可以停止的条件

满足以下条件时应考虑停止：

- 当前 Requirement 已能可靠 Match；
- STORY_GAP 已解决；
- 已确认转为 SKILL_GAP；
- 已确认转为 EVIDENCE_GAP；
- P0 问题已解决；
- 剩余 Unknown 不影响当前岗位决策；
- 继续追问的信息收益明显降低。

## 11.2 不应停止的条件

如果：

```text
MUST_HAVE Requirement
仍然 UNKNOWN
```

并且存在一个合理问题可以明显减少不确定性：

```text
应继续询问
```

---

# 12. Experience Asset

004 最终不是输出聊天记录，而是输出：

```text
ExperienceAsset
```

至少包含：

```text
ExperienceAsset
├── Refined Facts
├── Experience Story
├── Matched Requirements
├── Remaining Gaps
├── Upgrade Signals
├── Claim Suggestions
├── Evidence Requirements
└── Open Questions
```

核心原则：

> **Conversation 是手段，Experience Asset 才是产品。**

---

# 13. Experience Builder Web UI

004 增加 Experience Builder 工作区。

至少展示：

```text
Target Job
Current Requirement
Current Match / Gap
Relevant Experience
Agent Question
User Answer
Fact Candidate
Story Preview
Remaining Gap
```

用户能够：

```text
回答
跳过
补充说明
不知道
```

“我不知道”是合法答案。

Fact Candidate 支持：

```text
Confirm
Edit
Reject
```

Story Preview 支持：

```text
Accept
Edit
Request Rewrite
```

同时显示：

```text
Original Gap
Current Gap
Resolved / Remaining
Next Recommended Action
```

---

# 14. Core Business Rules

```text
R-001
Experience Mining ≠ Experience Creation
```

只能发现或澄清真实经历。

```text
R-002
Target Job drives the questions
```

不做无边界职业访谈。

```text
R-003
Broad Apply does not run full Experience Mining by default
```

控制成本。

```text
R-004
Ask 1～3 high-value questions per round
```

避免长问卷。

```text
R-005
Do not repeat confirmed questions
```

已确认事实不重复询问。

```text
R-006
User Answer ≠ Agent Structured Fact
```

Agent 新产生的结构化事实必须确认。

```text
R-007
Narrative ≠ Fact
```

润色不能创造事实。

```text
R-008
STORY_GAP → Mining
SKILL_GAP → Upgrade Signal → 005 UpgradeCandidate
EVIDENCE_GAP → Evidence Requirement → 005 / 006
```

保持职责分离，并避免 004 抢占 005 的 UpgradeCandidate 或 006 的 CandidateClaim 所有权。

```text
R-009
Agent cannot end workflow by itself
```

停止必须满足 Workflow / Rule 条件。

```text
R-010
Unknown is allowed
```

信息不足时不强行补全。

---

# 15. Acceptance Scenarios

## AC-001 — Story Gap Mining

Given：

```text
Requirement:
Agent Workflow

Experience:
“做过 LangGraph Agent”
```

When：

```text
系统发现信息不足
```

Then：

```text
提出与 Workflow / State / Recovery 有关的高价值问题
```

用户回答后：

```text
生成 Fact Candidate
用户确认
Experience 更新
Match 重新计算
```

## AC-002 — Skill Gap Routing

Given：

```text
JD 要求 MCP
```

When 用户明确：

```text
没有实际做过 MCP
```

Then：

```text
不继续 Experience Mining
Gap = SKILL_GAP
输出 Upgrade Signal
由 005 创建 UpgradeCandidate
```

## AC-003 — Evidence Gap Routing

Given：

```text
用户已确认做过 RAG Evaluation
```

But：

```text
没有可验证结果
```

Then：

```text
Gap = EVIDENCE_GAP
生成 Evidence Requirement
不继续无意义追问
```

## AC-004 — No Repeated Question

Given：

```text
用户已经确认使用过 LangGraph Checkpointer
```

Then：

后续 Mining 不得再次询问：

```text
是否使用过 Checkpointer？
```

除非出现事实冲突。

## AC-005 — Agent Fact Confirmation

用户回答：

```text
“我们当时用 BM25 + Milvus，再做 BGE Rerank”
```

Agent 生成：

```text
使用 Hybrid Retrieval，并通过 BGE Reranker 二阶段排序
```

Then：

```text
必须等待用户确认
```

确认前不能成为最终 Career Fact。

## AC-006 — Narrative Rewrite

Given：

```text
所有基础事实已经确认
```

When：

```text
Agent 重写 Experience Story
```

Then：

```text
可以直接生成 Narrative Preview
不需要逐句 Fact Confirmation
```

但不得增加新事实。

## AC-007 — Stop Condition

Given：

```text
MUST_HAVE Requirement 已经能可靠判断
所有 P0 问题已解决
剩余问题只影响 Story 丰富度
```

Then：

```text
Experience Mining 应停止
```

## AC-008 — Broad Apply

Given：

```text
Workflow = BROAD_APPLY
```

Then：

```text
默认不进入完整 Experience Mining
```

除非需要极少量关键澄清。

## AC-009 — Experience Asset

完成一次 Mining 后，系统输出：

```text
Refined Facts
Experience Story
Matched Requirements
Remaining Gaps
Upgrade Signals
Claim Suggestions
Evidence Requirements
Open Questions
```

而不是只有 Chat History。

---

# 16. Edge Cases

### User Does Not Remember

用户回答：

```text
记不清了
```

系统应：

```text
保留 UNKNOWN
```

不能推测补齐。

### Conflicting Answers

如果新的用户回答与已有 Confirmed Fact 冲突：

```text
不得自动覆盖
```

应提示用户选择或修正。

### Agent Over-interprets

如果 Agent 从用户一句普通描述推导出过度结论：

```text
用户可 Edit / Reject
```

拒绝结果不得写入 Career Fact。

### Too Many Open Questions

如果一个 Experience 存在大量缺失信息：

```text
只围绕当前 Target Requirement 追问
```

不试图一次补全整个职业历史。

### Mining Cannot Resolve Gap

多轮追问后仍不能确认：

```text
保持 UNKNOWN
或
明确转为 SKILL_GAP / EVIDENCE_GAP
```

不得为了流程闭环制造结论。

---

# 17. Definition of Done

004 Experience Builder 完成必须满足：

- [ ] Targeted Apply 可以根据 Gap 决定是否进入 Experience Mining；
- [ ] Broad Apply 默认不运行完整 Experience Mining；
- [ ] STORY_GAP 默认进入 Experience Mining；
- [ ] UNKNOWN / PARTIAL_MATCH 可以在信息不足时进入 Mining；
- [ ] 已确认 SKILL_GAP 不继续无意义追问；
- [ ] EVIDENCE_GAP 输出 Evidence Requirement，并按类型路由到 005 或 006；
- [ ] 每轮最多提出少量高价值问题；
- [ ] 问题由 Target Job / Requirement / Gap 驱动；
- [ ] 系统避免重复询问已确认事实；
- [ ] 用户原始回答保留为 USER_INPUT；
- [ ] Agent 结构化 Fact Candidate 必须用户确认；
- [ ] Narrative 可以基于已确认事实生成；
- [ ] Narrative 不得创造 Career Fact；
- [ ] Mining 具备明确 Stop Condition；
- [ ] SKILL_GAP 输出 Upgrade Signal，由 005 创建正式 UpgradeCandidate；
- [ ] EVIDENCE_GAP 可以输出 Evidence Requirement；
- [ ] ExperienceAsset 只输出 ClaimSuggestion，不创建正式 CandidateClaim；
- [ ] 最终产物统一为 Experience Asset；
- [ ] Web UI 支持追问、回答、Fact Review、Story Preview 和 Gap 查看；
- [ ] Acceptance Scenarios 通过；
- [ ] 未提前实现 Project Upgrade / Evidence Validation / Resume。

最终验收 Demo：

```text
Target Job
→ Requirement
→ PARTIAL_MATCH / STORY_GAP
→ Experience Builder
→ 1～3 个高价值问题
→ 用户回答
→ Fact Candidate
→ 用户确认
→ Experience 更新
→ Re-match
→ STORY_GAP Resolved
→ Remaining SKILL_GAP
→ Upgrade Signal
→ 005 creates UpgradeCandidate
→ Experience Asset
```

如果 RoleOS 能够稳定地区分：

```text
“用户没说清楚”
和
“用户确实没做过”
```

则：

> **004 Experience Builder 验收通过。**
