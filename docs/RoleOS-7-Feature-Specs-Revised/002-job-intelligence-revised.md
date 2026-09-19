# 002 — Job Intelligence

> **Feature ID**：002  
> **Feature Name**：Job Intelligence  
> **Product**：RoleOS  
> **Stage**：V1  
> **Status**：Revised after Cross-Feature Consistency Review

---

# 1. 目标

Job Intelligence 是 RoleOS V1 的岗位发现与岗位分流能力。

它解决三个核心问题：

```text
哪些岗位值得进入候选池？
哪些岗位适合 Broad Apply？
哪些岗位值得升级为 Targeted Apply？
```

本 Feature 完成后，RoleOS 应能够从真实招聘来源获取岗位，完成标准化、去重、硬过滤、轻量分析和排序，并让用户决定：

```text
Broad Apply
Targeted Apply
Skip
```

核心原则：

> **低价值岗位走低成本链路，高价值岗位才进入深度链路。**

---

# 2. 用户与范围

V1 首个真实 Job Source：

```text
Boss 直聘
```

002 负责：

- 从 Boss 直聘获取真实 Job；
- Job 标准化；
- Job 去重；
- Hard Filter；
- Lightweight JD Analysis；
- Broad Apply Ranking；
- Targeted Candidate 推荐；
- Broad / Targeted / Skip 用户决策；
- Browser Provider 基础接入；
- Job Pool Web UI；
- Job 获取失败后的可恢复处理。

本 Feature 不负责：

- Deep Experience Match；
- Experience Mining；
- Project Upgrade；
- Claim-Evidence；
- Resume Compiler；
- Application Submit；
- Outcome Feedback；
- 多站点聚合；
- 复杂 Ranking Model；
- Embedding 去重。

---

# 3. 核心 User Stories

## US-002-01 — 发现真实岗位

作为 RoleOS 用户，我希望系统可以从 Boss 直聘获取真实岗位，并保存为统一 Job 结构，以便后续所有 Match 和 Career Workflow 基于稳定数据工作。

## US-002-02 — 自动过滤低价值岗位

作为用户，我希望 RoleOS 能自动过滤明显不符合城市、薪资、经验等基本条件的岗位，减少无效岗位处理成本。

## US-002-03 — Broad Apply Ranking

作为用户，我希望 RoleOS 能对剩余岗位进行轻量分析和排序，让我优先查看最值得 Broad Apply 的岗位。

## US-002-04 — Targeted Candidate 推荐

作为用户，我希望系统能够识别值得深挖的高价值岗位，并建议将其升级为 Targeted Apply，但最终是否升级由我决定。

## US-002-05 — Job Pool 管理

作为用户，我希望在 Web UI 中看到候选岗位、过滤结果、Ranking、Broad / Targeted 状态和岗位详情，并可以执行 Promote 或 Skip。

---

# 4. Job Model

RoleOS 内部使用统一 Job 结构，不能让后续 Domain 直接依赖 Boss 原始页面结构。

`Job` 只描述外部岗位事实，不保存用户对该岗位的 Broad / Targeted 决策。

至少包含：

```text
Job
├── userId
├── source
├── externalJobId
├── title
├── company
├── city
├── salary
├── experienceRequirement
├── educationRequirement
├── rawJd
├── normalizedJd
├── publishTime
├── recruiterActivity
├── sourceUrl
├── sourceStatus
├── contentHash
└── metadata
```

用户如何处理这个 Job，由独立的 `JobCandidate / JobDecision` 保存：

```text
JobCandidate
├── userId
├── jobId
├── filterResult
├── jobRankingScore
├── strategy
├── strategyRecommendation
├── decisionReason
└── metadata
```

必须保持：

```text
Job = 岗位事实
JobCandidate.strategy = 用户当前求职策略
```

这样修改 Broad / Targeted 策略不会篡改 Canonical Job 本身。

其中：

```text
source = BOSS
```

V1 必须保留：

```text
raw source data
+
normalized fields
```

目的：

- 后续页面字段变化时可重新解析；
- Job Analysis 可追溯到原始 JD；
- 避免标准化过程中丢失信息。

---

# 5. Job Discovery & Normalization

## 5.1 Job Discovery

RoleOS 至少支持从 Boss 直聘获取：

```text
Job List
→ Job Detail
```

V1 不追求大规模爬取。

优先目标：

> **低频、可观察、面向单用户真实求职。**

系统不得依赖绕过验证码、风控或平台安全控制来完成 Job Discovery。

---

## 5.2 Job Normalization

Boss 原始岗位信息必须转换为 RoleOS Canonical Job。

标准化至少覆盖：

- Job Title；
- Company；
- City；
- Salary；
- Experience Requirement；
- Education Requirement；
- JD；
- Publish Time；
- Recruiter Activity；
- Source URL。

如果某字段缺失：

```text
UNKNOWN
```

而不是推断或虚构。

---

# 6. Job Dedup

V1 使用两层去重。

## 6.1 第一层：Source Identity

```text
source + externalJobId
```

如果完全一致，视为同一 Job。

## 6.2 第二层：Content Identity

如果 externalJobId 不同或来源数据变化，则使用：

```text
company
+
title
+
city
+
JD hash
```

进行第二层判断。

V1 不使用 Embedding Dedup。

核心目标：

> **避免相同岗位重复进入 Job Pool 和 Ranking。**

---

# 7. Hard Filter

Hard Filter 处理确定性条件。

V1 至少包括：

```text
City
Salary
Experience
Target Role
```

结果：

```text
PASS
REJECT
UNKNOWN
```

---

## 7.1 City

如果 Job 明确要求的城市与用户目标城市冲突：

```text
REJECT
```

如果 Job 支持 Remote / Multiple Cities：

```text
按实际信息判断
```

如果城市信息缺失：

```text
UNKNOWN
```

---

## 7.2 Salary

如果岗位明确薪资低于用户最低接受范围：

```text
REJECT
```

如果薪资缺失或表达模糊：

```text
UNKNOWN
```

不得自动假设满足要求。

---

## 7.3 Experience Requirement

如果岗位经验要求与用户背景存在明显硬冲突：

```text
REJECT
```

但经验年限只作为基础门槛，不替代后续能力匹配。

例如：

```text
要求 3 年
用户 5 年
```

只能说明基本满足年限。

不能说明技术能力一定匹配。

---

## 7.4 Target Role

如果 Job 明显不属于用户当前目标职业方向：

```text
REJECT
```

例如用户目标是：

```text
AI Agent Engineer
```

但岗位本质是：

```text
传统运营
纯销售
纯设计
```

则不应进入后续 Ranking。

---

# 8. Broad / Targeted Strategy

RoleOS 对 `JobCandidate` 使用两种岗位处理策略：

```text
BROAD_APPLY
TARGETED_APPLY
```

外加用户决策：

```text
SKIP
```

`strategy` 属于 JobCandidate / JobDecision，而不是 Canonical Job。

---

## 8.1 Broad Apply

Broad Apply 的目标：

> **用较低成本扩大有效岗位覆盖。**

Broad 流程：

```text
Job Discovery
→ Normalize
→ Dedup
→ Hard Filter
→ Lightweight JD Analysis
→ Lightweight Match
→ Ranking
→ User Review
```

Broad Apply 默认不进入：

- Experience Mining；
- Project Upgrade；
- Job-specific Resume；
- Deep Gap Analysis。

---

## 8.2 Targeted Apply

Targeted Apply 的目标：

> **对少量高价值岗位投入更深分析和真实能力升级。**

Targeted Apply 后续允许进入：

```text
Deep JD Analysis
→ Experience Match
→ Gap Analysis
→ Experience Builder
→ Project Upgrade
→ Job-specific Resume
```

这些能力由 Feature 003～006 实现。

---

## 8.3 最终决定权

RoleOS 可以推荐：

```text
Promote to Targeted
```

但不能自动完成策略升级。

必须经过用户确认。

流程：

```text
Broad Candidate
→ System Recommendation
→ User Review
→ Promote / Keep Broad / Skip
```

最终决定权属于用户。

---

# 9. Ranking Model

V1 使用：

> **确定性 Rule + Agent Semantic Judgment 的混合 Ranking。**

不提前建设复杂机器学习 Ranking Model。

---

## 9.1 Rule-based Signals

适合使用代码直接判断：

```text
City Fit
Salary Fit
Experience Fit
Publish Recency
Recruiter Activity
```

---

## 9.2 Agent-based Signals

适合 Agent 判断：

```text
Technical Stack Fit
Role Relevance
Career Goal Fit
Job Quality
Potential Experience Leverage
```

Agent 必须返回结构化结果和解释。

---

## 9.3 Recruiter Activity

招聘方活跃度纳入 Ranking，但只作为：

```text
Weak Signal
```

例如：

```text
刚刚活跃
今日活跃
近 3 日活跃
较久未活跃
```

活跃度可以影响排序，但不能压过：

```text
Role Fit
Skill Relevance
Salary
Career Goal
```

---

## 9.4 Ranking Output

每个 JobCandidate 至少输出：

```text
jobRankingScore
strategyRecommendation
reason
importantSignals
warnings
```

例如：

```text
JobRankingScore: 82

Recommendation:
PROMOTE_TO_TARGETED

Reasons:
- AI Agent 技术方向高度匹配
- Java / Spring 背景匹配
- 明确要求 MCP / RAG
- 薪资符合目标区间
- Recruiter 今日活跃

Warnings:
- 要求 Evaluation 经验，目前可能存在 Skill Gap
```

用户必须能够理解：

> 为什么这个 Job 排在前面。

---

# 10. Browser Boundary

002 是第一个真正使用 Browser 能力的 Feature。

架构边界必须保持：

```text
Job Intelligence
→ JobSiteAdapter
→ BrowserRouter
→ BrowserProvider
```

Career Domain 不直接依赖：

```text
Playwright
Kimi WebBridge
CSS Selector
XPath
Browser Internal Handle
```

---

## 10.1 Primary / Fallback

V1：

```text
Primary:
Playwright MCP

Fallback:
Kimi WebBridge
```

也允许用户手动指定 Provider。

---

## 10.2 Provider Switch

允许：

```text
Playwright Failure
→ BrowserRouter
→ Kimi WebBridge
```

但禁止：

```text
Playwright ElementRef
→ Kimi click
```

Provider 切换后必须通过业务状态恢复：

```text
JobSearchState
→ Navigate
→ Snapshot
→ Resume Business Step
```

---

## 10.3 Human Verification

遇到：

```text
Captcha
Human Verification
Platform Risk Warning
Sensitive Confirmation
```

必须：

```text
PAUSED_FOR_HUMAN
```

RoleOS 不绕过。

---

# 11. Job Pool Web UI

002 必须提供基础 Job Pool 页面。

至少支持：

```text
Job List
Job Detail
Filter Status
Job Ranking Score
Ranking Explanation
Broad / Targeted Strategy
Source
Recruiter Activity
Publish Time
Fetch Status
```

用户可以执行：

```text
Promote to Targeted
Keep Broad
Skip
Refresh
Open Source Job
```

---

## 11.1 Job List

至少可以按以下信息浏览：

```text
Title
Company
City
Salary
Score
Strategy
Recruiter Activity
Publish Time
```

支持基础筛选：

```text
Broad
Targeted
Skipped
PASS
UNKNOWN
```

---

## 11.2 Job Detail

至少展示：

```text
Raw / Normalized JD
Hard Filter Result
Ranking Signals
Ranking Explanation
Current Strategy
Source Link
```

后续 Feature 003 会在这里逐步增加：

```text
Requirement
Experience Match
Gap
```

002 不提前实现。

---

# 12. Core Business Rules

以下规则属于 002 的核心不变量。

```text
R-001
Boss Raw Data ≠ Canonical Job
```

必须经过标准化。

```text
R-002
Missing Data ≠ Matched
```

缺失信息使用 UNKNOWN。

```text
R-003
Hard Filter uses deterministic rules
```

不需要让 Agent 判断明确的城市 / 薪资等条件。

```text
R-004
Job Ranking Score must be explainable
```

不能只保存一个数字。

```text
R-005
Recruiter Activity is a weak signal
```

不能主导整个 Ranking。

```text
R-006
Broad Apply ≠ Targeted Apply
```

两条链路成本必须不同。

```text
R-007
System recommends, user decides
```

Targeted Promotion 最终由用户确认。

```text
R-008
Browser Provider ≠ Career Domain
```

Job Domain 不直接绑定 Browser Runtime。

```text
R-009
Provider Switch ≠ Session Migration
```

Provider 切换通过 Business State 恢复。

```text
R-010
Captcha → PAUSED_FOR_HUMAN
```

不绕过安全控制。

---

# 13. Acceptance Scenarios

## AC-001 — Boss Job Discovery

Given：

```text
用户已经拥有 Career Profile
```

When：

```text
RoleOS 从 Boss 直聘执行一次真实 Job Search
```

Then：

```text
至少获取一批真实 Job
保存 Raw Source Data
生成 Canonical Job
记录 source = BOSS
```

---

## AC-002 — Hard Filter

给定 20 个 Candidate Jobs，其中包含：

- 城市明显不匹配；
- 薪资明显低于要求；
- 经验要求明显冲突；
- 信息缺失。

系统能够分别得到：

```text
PASS
REJECT
UNKNOWN
```

并给出原因。

---

## AC-003 — Dedup

对于重复岗位：

```text
source + externalJobId 相同
```

不得重复进入 Job Pool。

如果 externalJobId 不同，但：

```text
company + title + city + JD hash
```

高度一致，也应识别为重复。

---

## AC-004 — Broad Ranking

Hard Filter 后剩余 Job：

```text
→ Lightweight Analysis
→ Ranking
```

用户可以看到：

```text
Score
Recommendation
Reason
Important Signals
Warnings
```

---

## AC-005 — Recruiter Activity

两个岗位其他条件接近：

```text
Job A recruiter = 今日活跃
Job B recruiter = 很久未活跃
```

活跃度可以影响排序。

但如果 Job B 在技术方向和 Career Goal 上明显更匹配：

```text
不能仅因为 Job A 更活跃就强制排在前面
```

---

## AC-006 — Targeted Promotion

Given：

```text
一个 Broad Candidate 被系统判断为高价值
```

Then：

```text
RoleOS 推荐 Promote to Targeted
```

But：

```text
Strategy 不自动改变
```

When：

```text
用户确认 Promote
```

Then：

```text
JobCandidate Strategy = TARGETED_APPLY
```

---

## AC-007 — Browser Fallback

Given：

```text
Playwright MCP 执行 Job Search
```

When：

```text
发生可恢复 Browser Failure
```

Then：

```text
BrowserRouter 可以选择 Kimi WebBridge
```

并通过业务状态重新进入当前 Job Search。

旧 Provider ElementRef 不得复用。

---

## AC-008 — Human Verification

When：

```text
Boss 页面出现 Captcha / Human Verification
```

Then：

```text
Workflow → PAUSED_FOR_HUMAN
```

用户处理后可以恢复。

系统不尝试自动绕过。

---

## AC-009 — Job Pool Web UI

用户可以通过 Web UI：

```text
查看 Job Pool
查看 Job Detail
查看 Filter Result
查看 Ranking Explanation
查看 Broad / Targeted 状态
Promote to Targeted
Keep Broad
Skip
打开 Boss 原始岗位页面
```

---

# 14. Edge Cases

### Job Field Missing

Salary、Experience、Education 等字段缺失时：

```text
UNKNOWN
```

不得根据职位标题自动补值。

### Boss 页面变化

页面字段获取失败时：

```text
Job Fetch 可以失败
Career Workflow 核心状态不能丢失
```

已经保存的 Job 不受影响。

### Duplicate Job Updated

同一个 Boss Job 后续 JD 发生修改：

```text
应更新当前 Job Snapshot
并保留可识别的更新时间 / Hash 变化
```

V1 不要求完整历史版本系统。

### Ranking Agent Failure

如果 Semantic Ranking Agent 暂时失败：

```text
已有 Hard Filter / Rule Signals 仍然保留
Job 不应消失
```

可标记：

```text
semanticAnalysisStatus = FAILED / PENDING
```

### User Overrides Recommendation

系统推荐：

```text
TARGETED
```

用户仍可以：

```text
Keep Broad
Skip
```

系统必须尊重用户决定。

---

# 15. Definition of Done

002 Job Intelligence 完成必须满足：

- [ ] Boss 直聘作为首个真实 Job Source 可用；
- [ ] 可以获取真实 Job List / Job Detail；
- [ ] Boss 原始数据可以标准化为 Canonical Job；
- [ ] Canonical Job 不直接保存 Broad / Targeted Strategy；
- [ ] JobCandidate / JobDecision 保存用户对岗位的 Strategy、filterResult 与 jobRankingScore；
- [ ] Raw Source Data 可追溯；
- [ ] 支持两层 Job Dedup；
- [ ] Hard Filter 支持 PASS / REJECT / UNKNOWN；
- [ ] City / Salary / Experience / Target Role 使用确定性规则；
- [ ] Broad Apply 使用 Lightweight Analysis；
- [ ] Ranking 使用 Rule + Agent 混合模式；
- [ ] jobRankingScore 与 Ranking Explanation 可解释；
- [ ] Recruiter Activity 被纳入但仅作为弱信号；
- [ ] RoleOS 可以推荐 Targeted Candidate；
- [ ] Targeted Promotion 必须由用户最终确认；
- [ ] Playwright MCP 作为 Primary Browser；
- [ ] Kimi WebBridge 可作为 Fallback / Manual Provider；
- [ ] Provider 切换不迁移 ElementRef / Session；
- [ ] Captcha / Human Verification 会暂停给用户；
- [ ] Job Pool Web UI 可完成核心操作；
- [ ] Acceptance Scenarios 通过；
- [ ] 未提前实现 Feature 003～007 的深度能力。

最终验收 Demo：

```text
进入 RoleOS
→ 从 Boss 直聘获取真实岗位
→ Normalize
→ Dedup
→ Hard Filter
→ Lightweight Analysis
→ Ranking
→ Job Pool
→ 用户查看 Top Jobs
→ 系统推荐一个 Targeted Candidate
→ 用户确认 Promote
→ JobCandidate Strategy = TARGETED_APPLY
```

并至少验证一次：

```text
Playwright Failure
→ BrowserRouter
→ Kimi WebBridge
→ 从 Business State 恢复
```

如果以上链路稳定运行：

> **002 Job Intelligence 验收通过。**
