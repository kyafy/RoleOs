# 003 — JD & Experience Match

> **Feature ID**：003  
> **Feature Name**：JD & Experience Match  
> **Product**：RoleOS  
> **Stage**：V1  
> **Status**：Revised after Cross-Feature Consistency Review

---

# 1. 目标

JD & Experience Match 是 RoleOS V1 的核心判断能力。

它解决三个问题：

```text
这个岗位到底要求什么？
用户哪些真实 Experience / Project 可以支撑这些要求？
还缺什么，缺口属于 Story、Skill 还是 Evidence？
```

本 Feature 完成后，RoleOS 应能够把一个真实 Job 的 JD 拆成结构化 Requirement，并基于用户已经确认的 Career Profile、Experience、Project、Skill 和 Capability，形成可解释的 Match 与 Gap。

核心流程：

```text
JD
→ Requirement Extraction
→ Experience Retrieval
→ Experience Match
→ Score + Status
→ Gap Analysis
→ Upgrade Feasibility
```

核心原则：

> **Retrieval 负责找到最值得比较的 Experience / Project；Match 负责判断它们是否真正满足 Requirement。**

---

# 2. 范围

003 负责：

- JD Requirement Extraction；
- Requirement Priority；
- Requirement Source Trace；
- Experience / Project Candidate Retrieval；
- 多 Experience / Project 联合匹配；
- Requirement Match Score；
- Match Status；
- STORY_GAP；
- SKILL_GAP；
- EVIDENCE_GAP；
- Upgrade Feasibility；
- Job Detail Match View。

本 Feature 不负责：

- Experience Mining；
- 用户动态追问；
- Project Upgrade 执行；
- Codex 调用；
- Evidence Validation；
- Resume Compiler；
- Application；
- Outcome Feedback；
- Embedding / Vector DB；
- Knowledge Graph；
- Semantic Reranker。

---

# 3. 核心 User Stories

## US-003-01 — 理解岗位要求

作为 RoleOS 用户，我希望系统能够把一份真实 JD 拆解成结构化 Requirement，并区分哪些要求是必须项、重要项、加分项和可选项。

## US-003-02 — 找到最相关经历

作为用户，我希望系统能够从我的 Career Profile 中找出最值得拿来匹配当前 Requirement 的 Experience / Project，而不是把所有历史经历都交给 Agent。

## US-003-03 — 判断真实匹配程度

作为用户，我希望 RoleOS 能告诉我每条 Requirement 当前是已经满足、部分满足、可通过项目升级补齐，还是不匹配，并给出 Score 和解释。

## US-003-04 — 识别不同 Gap

作为用户，我希望系统能够区分：

```text
Story Gap
Skill Gap
Evidence Gap
```

以便后续进入正确的 Experience Builder、Project Upgrade / Technical Evidence Build 或 Evidence Intake 流程。

---

# 4. Requirement Model

Requirement 表示：

> **从 Job JD 中提取出的结构化岗位要求。**

至少包含：

```text
Requirement
├── jobId
├── type
├── nature
├── capability
├── skills
├── priority
├── expectedDepth
├── expectedExperience
├── expectedEvidence
├── sourceText
├── confidence
└── metadata
```

其中：

```text
sourceText
```

必须保留，用于把 Requirement 追溯到原始 JD。

---

## 4.1 Requirement Priority

V1 使用四档：

```text
MUST_HAVE
IMPORTANT
NICE_TO_HAVE
OPTIONAL
```

含义：

| Priority | 含义 |
|---|---|
| MUST_HAVE | 明显属于岗位核心门槛 |
| IMPORTANT | 对岗位胜任度影响很大 |
| NICE_TO_HAVE | 有则明显加分，但不是核心门槛 |
| OPTIONAL | 辅助能力或低优先级要求 |

Priority 用于后续 Match Interpretation 和 Gap Priority。

## 4.2 Requirement Nature

Requirement 还需要区分“要求的本质是什么”，避免把真实历史条件误当成普通 Skill Gap。

V1 使用：

```text
CAPABILITY
HISTORICAL_EXPERIENCE
CONTEXTUAL
EVIDENCE
```

例如：

```text
“熟悉 MCP”
→ CAPABILITY

“5 年团队管理经验”
→ HISTORICAL_EXPERIENCE

“有大型企业生产环境经验”
→ CONTEXTUAL / HISTORICAL_EXPERIENCE

“能够提供可验证的 Evaluation 结果”
→ EVIDENCE
```

Requirement Nature 与 Gap Type 是两个正交维度。

---

# 5. Requirement Extraction

Requirement Extraction 的目标不是简单做关键词提取，而是把 JD 转换成 RoleOS 可处理的岗位能力结构。

例如 JD：

```text
熟悉 RAG、Agent Workflow 和 MCP，
有实际项目经验，
具备 Evaluation 和可观测性经验优先。
```

可以拆成：

```text
R1
Capability:
RAG Engineering
Priority:
MUST_HAVE

R2
Capability:
AI Agent Engineering
Skills:
Agent Workflow
Priority:
MUST_HAVE

R3
Skill:
MCP
Priority:
IMPORTANT

R4
Skill:
Evaluation
Priority:
NICE_TO_HAVE

R5
Skill:
Observability
Priority:
NICE_TO_HAVE
```

每个 Requirement 必须保留对应原始 JD 片段。

---

# 6. Match Mode

003 必须遵循 002 已经确定的 Broad / Targeted 成本策略。

## 6.1 BROAD_MATCH

用于 `BROAD_APPLY`，只做低成本判断：

```text
Lightweight Requirement Extraction
→ Structured Retrieval
→ Lightweight Match Summary
```

主要服务：

```text
Job Ranking
Promote to Targeted Decision
```

Broad Match 默认不执行完整 Deep Gap Analysis、Upgrade Feasibility 和 Experience Mining Preparation。

## 6.2 TARGETED_MATCH

用于 `TARGETED_APPLY`，执行完整分析：

```text
Full Requirement Extraction
→ Multi-Experience Retrieval
→ Requirement Match
→ Gap Analysis
→ Upgrade Feasibility
```

除非用户主动要求，否则只有 Targeted Job 才进入完整深度链路。

---

# 7. Experience Retrieval

## 7.1 Retrieval 的职责

Retrieval 只负责回答：

> **哪些 Experience / Project 最值得进入下一步 Match？**

不负责判断：

```text
MATCHED
PARTIAL_MATCH
NOT_MATCHED
```

即：

```text
Retrieval
= Candidate Selection

Match
= Final Judgment
```

---

## 7.2 为什么需要 Retrieval

Career Profile 随着使用会不断积累：

```text
Experience
Project
Skill
Capability
Evidence
```

如果每次都把全部内容交给 Agent：

```text
Requirement
+
All Experiences
→ LLM
```

会导致：

- Context 变大；
- Token 成本增加；
- 无关 Experience 增加噪音；
- Match 可解释性降低。

因此先做 Candidate Retrieval。

---

## 7.3 V1 Retrieval Signals

V1 使用可解释的结构化 Retrieval。

主要参考：

```text
Skill Match
Capability Match
Technology Match
Experience / Project Text Match
Existing Evidence
```

可以理解成：

```text
Requirement
      ↓
Skill / Capability / Keyword
      ↓
Candidate Retrieval
      ↓
Top Relevant Experience / Project
```

V1 不要求固定具体权重。

实现应允许后续调整。

---

## 7.4 Retrieval Output

对于一个 Requirement：

```text
Requirement R1
```

Retrieval 返回：

```text
Candidate Experiences / Projects:
- E03
- P07
- P11
```

可以带内部 `retrievalRelevanceScore`，但该分数：

> **不是最终 `requirementMatchScore`。**

---

## 7.5 Top K

V1 默认只需要返回少量高相关 Candidate。

建议：

```text
Top 3～5
```

避免把所有 Experience 都送入深度 Match。

---

## 7.6 V1 不使用 Embedding

V1 Retrieval 使用：

```text
Structured Match
+
Keyword / Full-text Match
```

暂不引入：

```text
Embedding
Vector Database
Semantic Reranker
Knowledge Graph
```

后续只有在真实数据表明：

```text
Retrieval Recall 不足
```

时，再升级语义检索能力。

---

# 8. Experience Match

Match 负责回答：

> **某个 Requirement 当前到底被哪些 Experience / Project 支撑，以及满足程度如何。**

一个 Requirement 可以由多个 Experience / Project 联合支撑。

Skill Depth / Capability Depth 的判断证据优先级必须是：

```text
Verified Depth
>
Experience-supported Depth
>
Self-assessed Depth
```

Self-assessed Depth 可以用于 Retrieval 或生成澄清问题，但不能单独把 Requirement 判定为 `MATCHED`。

例如：

```text
Requirement:
RAG Engineering

Supporting:
Project A
+
Project B
```

Project A 可以支撑：

```text
Hybrid Retrieval
Rerank
```

Project B 可以支撑：

```text
Evaluation
```

最终联合形成 Requirement Match。

---

## 8.1 Match Status

V1 使用：

```text
MATCHED
PARTIAL_MATCH
NOT_MATCHED
UNKNOWN
```

Match Status 只回答：

> **当前 Requirement 到底匹不匹配？**

“是否值得/能够通过 Project Upgrade 补齐”由独立的 `UpgradeFeasibility` 表达，不再使用 `UPGRADABLE` 作为 Match Status。

---

### MATCHED

用户现有真实 Experience / Project 已经能够充分支撑该 Requirement。

---

### PARTIAL_MATCH

用户已经具备部分能力，但深度、完整性或表达不足。

---

### NOT_MATCHED

当前已知事实不足以满足该 Requirement。是否可升级由 `UpgradeFeasibility` 单独判断。

---

### UNKNOWN

现有 Career Profile 信息不足，无法可靠判断。

---

# 9. Requirement Match Score

除了 Match Status，V1 还保留：

```text
Requirement Match Score
```

Score 与 Status 是两种不同信息。

```text
requirementMatchScore
= 连续程度

Status
= 业务状态
```

例如：

```text
requirementMatchScore: 72
Status: PARTIAL_MATCH
```

比单独一个：

```text
72
```

更有解释力。

---

## 9.1 Requirement Match Score 的使用

Requirement Match Score 可以用于：

- Requirement Coverage；
- Job Match Summary；
- 前后版本对比；
- Project Upgrade 前后变化；
- 后续 Ranking 辅助。

`requirementMatchScore` 不应该作为唯一业务判断依据。

---

# 10. Gap Analysis

当 Requirement 不是完全 MATCHED 时，RoleOS 应判断 Gap 类型。

V1 只保留三类一级 Gap：

```text
STORY_GAP
SKILL_GAP
EVIDENCE_GAP
```

---

## 10.1 STORY_GAP

定义：

> 用户真实做过，但当前 Career Profile / Experience 表达不足，无法充分证明匹配。

例如：

```text
用户实际负责过 Agent Workflow 设计，
但 Experience 只写：
“参与 AI Agent 项目开发”
```

属于：

```text
STORY_GAP
```

后续进入：

```text
Experience Builder
```

---

## 10.2 SKILL_GAP

定义：

> 用户当前真实缺少该能力或实践深度不足。

例如：

```text
JD 要求 MCP Server / Client
用户从未实际做过 MCP
```

属于：

```text
SKILL_GAP
```

后续可能进入：

```text
Project Upgrade
```

---

## 10.3 EVIDENCE_GAP

定义：

> 用户声称具备能力，但缺少足够 Evidence 支撑。

例如：

```text
用户说做过 RAG 优化
但没有：
Test
Evaluation
Trace
Source Code
```

属于：

```text
EVIDENCE_GAP
```

后续输出：

```text
Evidence Requirement
```

并按 Evidence 缺口性质路由：

```text
需要新 Test / Eval / Trace / Benchmark / Demo
→ 005 Project Upgrade / Technical Evidence Build

已有历史材料需要接入、验证或关联 Claim
→ 006 Evidence Intake & Claim Review
```

003 本身不构建 Evidence。

---

# 11. Upgrade Feasibility

对于 SKILL_GAP，003 需要判断：

> **这个 Gap 是否适合通过真实 Project Upgrade（已有项目或新建真实项目）补齐？**

V1 使用：

```text
HIGH
MEDIUM
LOW
NOT_APPLICABLE
```

---

## 11.1 HIGH

例如：

```text
已有 RAG 项目
缺少 Rerank
```

通过真实项目增加：

```text
Rerank
+
Evaluation
```

可合理补齐。

---

## 11.2 MEDIUM

可以补齐，但可能需要：

- 较多代码改造；
- 新基础设施；
- 明显学习成本；
- 较长验证过程。

---

## 11.3 LOW

理论上可以做，但：

```text
成本高
复用价值低
对当前岗位帮助有限
```

不优先推荐。

---

## 11.4 NOT_APPLICABLE

以下 Gap 不得通过 Project Upgrade 伪装成真实历史经验：

```text
多年真实管理经验
真实企业生产规模
真实企业客户经验
真实行业从业年限
真实团队管理经历
真实商业结果
真实线上用户量
```

核心原则：

> **Project Upgrade 可以增加真实能力，但不能制造过去没有发生过的职业经历。**

---

# 12. Match Summary

对于一个 Target Job，RoleOS 应能够形成整体 Match Summary。

例如：

```text
Strong Matches
- Java / Spring Boot
- RAG Development

Partial Matches
- Agent Workflow

Upgradable Gaps
- MCP
- Evaluation

Story Gaps
- Architecture Decision
- Business Impact

Evidence Gaps
- RAG Quality Evaluation

Hard Gaps
- Large-scale Production Experience
```

整体 Match Summary 应能帮助后续 Workflow 决定：

```text
需要 Experience Mining？
需要 Project Upgrade？
需要 Technical Evidence Build / Evidence Intake？
还是当前不值得继续投入？
```

最终业务决策仍由后续 Workflow 和用户控制。

---

# 13. Job Detail Web UI

003 不新建复杂独立页面。

直接扩展 002 的 Job Detail。

至少增加：

```text
JD Requirements
Match Summary
Strong Matches
Partial Matches
Story Gaps
Skill Gaps
Evidence Gaps
Upgrade Feasibility
```

---

## 13.1 Requirement Detail

用户点击一个 Requirement 时，应看到：

```text
Requirement
↓
JD Source Text
↓
Supporting Experience / Project
↓
Requirement Match Score
↓
Match Status
↓
Reason
↓
Gap
↓
Upgrade Feasibility
```

例如：

```text
Requirement:
熟悉 MCP，并具备实际项目经验

Source:
JD 第 3 段

Supporting Experience:
Project A

Status:
NOT_MATCHED

Requirement Match Score:
45

Upgrade Feasibility:
HIGH

Reason:
当前项目具备 Agent Workflow / Tool Calling，
但没有 MCP Server / Client 实践。

Gap:
SKILL_GAP

Upgrade Feasibility:
HIGH
```

---

# 14. Core Business Rules

```text
R-001
Requirement must trace back to JD sourceText
```

不能产生无法追溯到 JD 的核心 Requirement。

```text
R-002
Retrieval ≠ Match
```

Retrieval 只找 Candidate，不做最终匹配结论。

```text
R-003
retrievalRelevanceScore ≠ requirementMatchScore
```

两个分数必须区分。

```text
R-004
One Requirement can use multiple Experiences / Projects
```

允许联合支撑。

```text
R-005
Score + Status must both exist
```

不能只保存单一 `requirementMatchScore`。

```text
R-006
Gap only uses STORY / SKILL / EVIDENCE
```

V1 不扩展大量一级 Gap 类型。

```text
R-007
Skill Gap does not automatically mean Project Upgrade
```

必须先判断 Upgrade Feasibility。

```text
R-008
Project Upgrade cannot fabricate historical experience
```

真实年限、管理经验、生产规模等不能通过 Portfolio Upgrade 补齐。

```text
R-009
UNKNOWN is valid
```

信息不足时允许 UNKNOWN，不要求 Agent 强行判断。

```text
R-010
V1 Retrieval remains explainable
```

暂不使用复杂不可解释 Retrieval Stack。

```text
R-011
BROAD_MATCH ≠ TARGETED_MATCH
```

Broad 只做低成本 Match Summary；完整 Gap Analysis 与 Upgrade Feasibility 默认属于 Targeted。

```text
R-012
Verified Depth > Experience-supported Depth > Self-assessed Depth
```

Self-assessment 不能单独把 Requirement 判为 MATCHED。

---

# 15. Acceptance Scenarios

## AC-001 — Requirement Extraction

Given：

```text
一个真实 AI Agent Engineer JD
```

When：

```text
RoleOS 执行 Requirement Extraction
```

Then：

```text
生成结构化 Requirement
每条 Requirement 有 Priority
每条 Requirement 可追溯 sourceText
```

---

## AC-002 — Candidate Retrieval

Given：

```text
Career Profile 中存在多个 Experience / Project
```

When：

```text
处理一个 MCP / Agent Workflow Requirement
```

Then：

```text
Retrieval 返回少量最相关 Candidate
不需要把所有 Experience 全部交给 Match
```

---

## AC-003 — Multi Experience Match

Given：

```text
Requirement:
RAG Engineering
```

And：

```text
Project A:
Hybrid Retrieval + Rerank

Project B:
Evaluation
```

Then：

```text
系统允许 A + B 联合支撑同一个 Requirement
```

---

## AC-004 — Score + Status

系统输出：

```text
requirementMatchScore
+
Status
+
Reason
+
Supporting Experience
```

不能只返回一个数值。

---

## AC-005 — Story Gap

Given：

```text
用户真实做过 Agent Workflow
但 Career Profile 描述过浅
```

Then：

```text
PARTIAL_MATCH
+
STORY_GAP
```

而不是直接判定：

```text
NOT_MATCHED
```

---

## AC-006 — Skill Gap

Given：

```text
JD 要求 MCP
用户没有任何 MCP 实践
```

Then：

```text
SKILL_GAP
```

并给出：

```text
Upgrade Feasibility
```

---

## AC-007 — Evidence Gap

Given：

```text
用户声称做过 RAG Optimization
```

But：

```text
没有 Test / Evaluation / Trace 等 Evidence
```

Then：

```text
EVIDENCE_GAP
```

---

## AC-008 — Non-upgradable Gap

Given：

```text
JD 要求 5 年真实团队管理经验
```

And：

```text
用户没有真实团队管理经历
```

Then：

```text
Match Status = NOT_MATCHED
Requirement Nature = HISTORICAL_EXPERIENCE
Gap = SKILL_GAP
Upgrade Feasibility = NOT_APPLICABLE
```

这里 `SKILL_GAP` 只表示当前条件无法被能力事实满足；真正说明“这是历史年限/经历要求”的字段是 `Requirement Nature`。

不得建议：

```text
通过 Personal Project 模拟 5 年管理经验
```

---

## AC-009 — Job Detail Match View

用户在 Web UI 打开 Job Detail 后，可以看到：

```text
Requirements
Match Status
Requirement Match Score
Supporting Experience
Gap
Upgrade Feasibility
Source JD Text
```

并能理解：

> 为什么系统认为自己匹配或不匹配。

---

# 16. Edge Cases

### Requirement Extraction Uncertain

如果 JD 表达模糊：

```text
优先级或能力含义不确定
```

可以：

```text
confidence = low
```

不得强行当成 MUST_HAVE。

---

### No Relevant Experience

如果 Retrieval 找不到任何相关 Experience：

```text
Candidate List = empty
```

Match 可以进入：

```text
NOT_MATCHED
或
UNKNOWN
```

取决于是否有足够信息做判断。

---

### Experience Information Incomplete

如果存在相关 Experience，但描述不足：

优先考虑：

```text
STORY_GAP
```

而不是直接 Skill Gap。

---

### Conflicting Evidence

如果用户 Self-assessed Depth 很高，但现有 Experience 支撑很弱：

```text
Match 仍以当前可确认事实为基础
```

不得仅使用 Self-assessment 提升 Match。

---

### Retrieval Miss

如果用户明显存在相关经历但 V1 Retrieval 没有召回：

允许人工：

```text
Add Supporting Experience
```

后续再根据真实数据优化 Retrieval。

---

# 17. Definition of Done

003 JD & Experience Match 完成必须满足：

- [ ] 能从真实 JD 提取结构化 Requirement；
- [ ] Requirement 使用 MUST_HAVE / IMPORTANT / NICE_TO_HAVE / OPTIONAL 四档；
- [ ] Requirement Nature 支持 CAPABILITY / HISTORICAL_EXPERIENCE / CONTEXTUAL / EVIDENCE；
- [ ] BROAD_MATCH 与 TARGETED_MATCH 使用不同分析深度；
- [ ] Requirement 可追溯到 JD sourceText；
- [ ] V1 提供结构化 Candidate Retrieval；
- [ ] Retrieval 使用 Skill / Capability / Technology / Text 等可解释信号；
- [ ] V1 不依赖 Embedding / Vector DB；
- [ ] retrievalRelevanceScore 与 requirementMatchScore 分离；
- [ ] 支持 Top Candidate Experience / Project；
- [ ] 一个 Requirement 支持多个 Experience / Project 联合支撑；
- [ ] Match 同时输出 Score + Status；
- [ ] Match Status 支持 MATCHED / PARTIAL_MATCH / NOT_MATCHED / UNKNOWN；
- [ ] Gap 支持 STORY_GAP / SKILL_GAP / EVIDENCE_GAP；
- [ ] SKILL_GAP 支持 HIGH / MEDIUM / LOW / NOT_APPLICABLE Upgrade Feasibility；
- [ ] UpgradeFeasibility 与 MatchStatus 职责分离，不再使用 UPGRADABLE MatchStatus；
- [ ] Skill Depth Match 遵循 Verified > Experience-supported > Self-assessed；
- [ ] EVIDENCE_GAP 能路由到 005 Technical Evidence Build 或 006 Evidence Intake；
- [ ] 真实管理年限、生产规模、客户经验等不能通过 Project Upgrade 伪造；
- [ ] Job Detail 可以查看 Requirement、Match、Gap 和来源；
- [ ] Acceptance Scenarios 通过；
- [ ] 未提前实现 Feature 004～007。

最终验收 Demo：

```text
真实 Target Job
+
真实 Career Profile
+
多个 Experience / Project
        ↓
JD Requirement Extraction
        ↓
Candidate Retrieval
        ↓
Multi-Experience Match
        ↓
Score + Status
        ↓
Story / Skill / Evidence Gap
        ↓
Upgrade Feasibility
        ↓
Job Detail 可解释展示
```

如果用户能够清楚回答：

```text
我为什么匹配这个岗位？
我哪里只是没讲清楚？
我真正缺什么能力？
哪些能力可以通过真实项目升级补齐？
```

则：

> **003 JD & Experience Match 验收通过。**
