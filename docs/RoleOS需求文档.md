# RoleOS 需求文档

## 1. 项目概述

### 1.1 RoleOS 是什么

RoleOS 是一套面向 **AI 应用开发工程师、AI Agent 开发工程师及相关技术岗位求职者** 的 AI Career Agent。

RoleOS 以目标岗位为中心，连接岗位发现、JD 分析、经历匹配、项目升级、简历生成、面试准备和求职投递，帮助用户持续提升与目标岗位的真实匹配度。

核心流程：

```text
岗位发现
→ JD 分析
→ 经历匹配
→ Gap Analysis
→ Experience Mining
→ Project Upgrade
→ Evidence
→ Resume / Interview
→ Application
→ Outcome Feedback
```

RoleOS 的核心目标不是帮助用户“投更多简历”，而是：

> **帮助用户找到值得投的岗位，并把真实经历升级到足以匹配目标岗位的程度。**

### 1.2 RoleOS 解决的问题

RoleOS 重点解决四类问题：

- **岗位选择**：将岗位搜索升级为岗位筛选、匹配分析和投递优先级决策。
- **经历深度不足**：通过 Experience Mining 和 Project Upgrade，将 Demo / 项目升级为更完整、可验证的 Portfolio Project。
- **简历只改文字，不提升能力**：区分 Story Gap、Skill Gap、Evidence Gap，能真实补齐的优先 Project Upgrade。
- **简历与面试脱节**：同步生成 Resume、Portfolio、Interview Story，确保写出来的内容用户能够真实解释。

### 1.3 RoleOS 不是什么

RoleOS 不定位为：

- 大规模招聘网站爬虫；
- 无人值守海投工具；
- 单纯的 AI 简历润色工具；
- Coding Agent 的替代品。

其中 Browser 负责网页执行，Codex 负责真实项目代码升级，RoleOS 负责职业决策、流程编排和经历建设。涉及经历真实性、项目重大升级、最终简历和正式投递时，保留用户确认。

### 1.4 核心能力

| 能力 | 作用 |
|---|---|
| **Job Intelligence** | 岗位发现、筛选、JD 分析和排序 |
| **Career Memory** | 管理用户长期职业画像和经历 |
| **Experience Intelligence** | 匹配 Job Requirement 与用户 Experience |
| **Experience Builder** | 深挖经历并识别 Story / Skill / Evidence Gap |
| **Project Upgrade** | 调用 Codex 真实升级已有项目并产生 Evidence |
| **Career Execution** | 生成 Resume / Interview Story，并辅助投递和结果跟踪 |

其中 **Experience Builder + Project Upgrade + Claim-Evidence** 是 RoleOS 最核心的差异化能力。

### 1.5 V1 用户与范围

RoleOS V1 面向正在求职 AI 应用开发工程师、AI Agent 开发工程师及相近技术岗位的工程师，第一阶段以开发者本人作为 Seed User。

V1 计划在 **6～8 周**内跑通最小完整闭环：

```text
真实 Job
→ JD Analysis
→ Experience Match
→ Gap Analysis
→ Experience Mining
→ Codex Project Upgrade
→ Evidence
→ Resume
→ Interview Story
→ Human Review
→ Application
```

V1 不追求覆盖所有招聘网站、支持所有职业类型、无人值守批量投递或完整企业级招聘平台。

### 1.6 核心产品原则

- **Target Role First**：所有优化围绕具体目标岗位展开。
- **Evidence First**：重要能力和结果尽量具备真实 Evidence。
- **Upgrade Before Rewrite**：能力不足优先真实补齐，而不是只优化文字。
- **Human in Control**：关键外部操作和真实性判断由用户最终确认。
- **Explainable by Design**：重要推荐和决策必须能够解释。
- **Feedback Driven**：投递和面试结果持续反向更新 Career Memory。

---

## 2. 术语与领域概念

### 2.1 岗位领域

| 术语 | 定义 |
|---|---|
| **Role** | 一类目标职业角色，例如 AI Agent Engineer |
| **Job** | 某家公司发布的具体招聘职位 |
| **JD** | Job 的原始岗位描述 |
| **Requirement** | 从 JD 中抽取出的结构化岗位要求 |

> **Role 是职业能力模型，Job 是具体招聘机会。**

Requirement 建议区分：`MUST_HAVE / IMPORTANT / NICE_TO_HAVE / OPTIONAL`。

### 2.2 用户职业领域

| 术语 | 定义 |
|---|---|
| **Career Profile** | 用户当前完整职业画像 |
| **Experience** | 用户真实发生过的工作、项目或学习经历 |
| **Project** | Experience 中可独立描述的工程项目 |
| **Portfolio Project** | 用于证明能力的展示型项目 |
| **Skill** | 单项技术或职业技能 |
| **Capability** | 由多个 Skill 组成的高层能力 |

重要边界：`Portfolio Project ≠ Production Experience`。

### 2.3 匹配与差距

**Experience Match** 表示某个 Requirement 与用户 Experience / Project 的匹配关系。

| Gap | 定义 | 主要处理方式 |
|---|---|---|
| **Story Gap** | 做过，但没有表达清楚 | Experience Mining |
| **Skill Gap** | 当前确实没有该能力 | Learning / Project Upgrade |
| **Evidence Gap** | 声称具备能力，但缺少证明 | Test / Eval / Evidence |

### 2.4 Experience Builder

- **Experience Mining**：通过针对性追问，从用户真实经历中挖掘业务背景、技术问题、决策和结果。
- **Experience Builder**：将真实 Experience、目标 Requirement、Experience Mining 和 Project Upgrade 组合成完整项目故事。
- **Project Upgrade**：为了补齐真实能力 Gap，对已有 Project 进行实际工程改造。
- **Upgrade Plan**：Project Upgrade 执行前的目标、范围和验证计划。

### 2.5 Claim 与 Evidence

**Claim** 表示简历或面试中关于用户能力、经历或结果的一条陈述。

**Evidence** 表示支持 Claim 的真实依据，例如：`USER_STATEMENT / SOURCE_CODE / GIT_COMMIT / TEST / EVALUATION / TRACE / ARCHITECTURE_DOCUMENT / DEMO`。

> 重要技术能力和量化结果应尽量能够追溯到 Evidence。

### 2.6 Career Memory

**Career Memory** 表示 RoleOS 长期维护的用户职业上下文。结构化职业事实进入 **Career Database**；偏好、上下文和历史经验进入 **Agent Memory**。

> **Database 管事实，Memory 管上下文。**

### 2.7 求职产物

| 术语 | 定义 |
|---|---|
| **Resume Version** | 针对目标 Job 生成的一份定制简历 |
| **Interview Story** | 与 Resume Claim 一致的完整项目面试故事 |
| **Application** | 用户对某个 Job 发起的一次正式投递 |
| **Outcome** | 已读、回复、面试、Reject、Offer 等后续结果 |

### 2.8 Workflow 与执行角色

- **Career Workflow**：可以持久化、暂停和恢复的长期业务流程。
- **Human Approval**：关键动作执行前的用户确认。
- **RoleOS Agent**：负责职业判断、规划、追问和流程协调。
- **Codex**：负责 Repo 分析、代码改造、Test、Evaluation 和技术文档。
- **Browser Provider**：负责招聘网站等网页的读取、点击、填写和上传。

```text
Career Workflow  → 管流程状态
RoleOS Agent     → 管判断
Codex            → 管技术建设
Browser          → 管网页执行
Career Memory    → 管用户长期信息
Evidence         → 管真实性
```

---

## 3. 设计目标与产品原则

RoleOS 的核心设计目标可以概括为：**匹配、真实、深度、可解释、可控、可进化**。

| 目标 | 定义 |
|---|---|
| **匹配 Match** | 围绕具体 Target Role / Job 展开，优先提升真实岗位匹配度 |
| **真实 Evidence-based** | 重要经历、能力和量化结果尽量由真实 Experience 或 Evidence 支撑 |
| **深度 Depth** | 深入到业务场景、技术决策、工程实现和项目结果 |
| **可解释 Explainable** | 岗位推荐、Gap 判断、项目升级和简历调整均可说明原因 |
| **可控 Human-controlled** | 重要项目修改、简历确认和正式投递由用户保留最终控制权 |
| **可进化 Adaptive** | 根据岗位、项目升级、投递和面试结果持续更新 Career Memory 与策略 |

### 3.1 核心产品原则

- **Target Role First**：不进行无目标的“简历优化”。
- **Evidence First**：优先使用真实事实和 Evidence 构建职业故事。
- **Upgrade Before Rewrite**：真实 Skill Gap 优先补齐，而不是用文字掩盖。
- **Agent for Judgment, Code for Rules**：理解、规划和判断交给 Agent，确定性任务优先使用代码。
- **Human in Control**：经历真实性、重大 Project Upgrade、Resume 最终版本、对外沟通、正式投递必须保留人工确认。
- **Feedback Driven**：Application → Outcome → Feedback → Career Memory → 下一轮策略。

### 3.2 V1 明确追求

V1 优先验证岗位发现与筛选、Requirement 抽取、Experience 匹配、三类 Gap 识别、Experience Mining、Codex 项目升级、Evidence 生成、Resume / Interview Story、辅助投递和 Outcome Feedback。

### 3.3 V1 明确不追求

不追求覆盖所有招聘网站、所有职业类型、自动批量海投、绕过验证码、完整 ATS、自研通用 Browser Runtime、自研 Coding Agent、复杂 Multi-Agent Network、完整 Career Knowledge Graph、自动生成不存在的项目经历或生产数据。

### 3.4 核心真实性边界

```text
真实经历
→ 深度挖掘
→ 业务场景强化
→ 项目真实升级
→ 专业表达
```

不允许：`不存在的经历 → 包装成真实经历`。

必须保持：`Portfolio Project ≠ Past Production Experience`，`Designed Scenario ≠ Real Company Scenario`。

---

## 4. 目标用户与典型场景

### 4.1 目标用户

RoleOS V1 主要面向正在求职 AI 应用开发工程师、AI Agent 开发工程师及相近技术岗位的工程师。典型用户有 Java / Python / 后端或 AI 应用基础，正在向 RAG、Agent、MCP、LangGraph 等方向发展，已有 Demo 或项目但缺少企业级项目深度。

### 4.2 场景一：发现值得投递的岗位

```text
岗位发现
→ 基础筛选
→ JD Analysis
→ Experience Match
→ 综合排序
```

输出 Top Jobs、匹配度、核心 Requirement、推荐理由和主要 Gap。

### 4.3 场景二：Demo 升级为高质量 Portfolio Project

```text
JD Requirement
→ Experience Match
→ Experience Mining
→ Gap Analysis
→ Upgrade Plan
→ Human Approval
→ Codex Project Upgrade
→ Test / Eval
→ Evidence
```

完成后生成 Resume Narrative、Portfolio、Interview Story。

### 4.4 场景三：从目标岗位到正式投递

```text
JD + Career Profile + Experience + Evidence
→ Resume Version
→ Interview Story
→ Application Preparation
→ Human Approval
→ Application
→ Outcome
```

### 4.5 V1 场景优先级

| 场景 | 优先级 | 作用 |
|---|---:|---|
| 岗位发现与排序 | P0 | 为后续流程提供真实 Target Job |
| Demo → Portfolio Project | **P0 / 核心** | 验证 RoleOS 最核心差异化价值 |
| 定制简历与投递 | P0 | 跑通端到端求职闭环 |
| 多招聘网站覆盖 | P1 | 扩展岗位来源 |
| 自动面试复盘 | P1 | 强化 Feedback Loop |
| 多用户 / 多职业类型 | P2 | 产品规模化阶段 |

---

# 5. 核心功能

## 5.1 Job Intelligence

### 5.1.1 目标

Job Intelligence 负责获取、解析、筛选和排序职位，并根据不同求职目标选择合适的处理策略。

```text
BROAD_APPLY      海投模式
TARGETED_APPLY   定向投递模式
```

两种模式共享岗位发现、标准化和基础过滤能力，但从 JD 分析、排序、简历策略和投递策略开始分叉。

> **既能以较低成本扩大面试机会，也能对高价值岗位进行深度定制。**

### 5.1.2 核心流程

```text
Job Discovery
      ↓
Job Normalize
      ↓
Hard Filter
      ↓
Search Strategy
   ┌───────────────┐
   ▼               ▼
BROAD_APPLY    TARGETED_APPLY
   │               │
Fast Analysis   Deep Analysis
   │               │
Fast Match      Experience Match
   │               │
Fast Rank       Gap Analysis
   │               │
Role Resume     Experience Mining
   │               │
Apply           Project Upgrade?
                   │
              Job-specific Resume
                   │
              Human Approval
                   │
                  Apply
```

### 5.1.3 Job Search Strategy

#### Broad Apply Mode

目标：**扩大有效投递覆盖面，获取更多合适的面试机会。**

强调较大的候选岗位池、较低单岗位处理成本、快速筛除不合适岗位、优先新发布/HR 活跃/基础匹配度较高的职位，并使用 Role-level Resume。

```text
大量 Candidate Jobs
→ Hard Filter
→ Fast JD Analysis
→ Basic Match
→ Fast Ranking
→ Role Resume
→ Human Batch Review
→ Apply
```

#### Targeted Apply Mode

目标：**对用户明确希望重点争取的高价值岗位进行深度定制。**

```text
Deep JD Analysis
→ Experience Match
→ Gap Analysis
→ Experience Mining
→ Project Upgrade
→ Evidence
→ Job-specific Resume
→ Interview Story
→ Human Approval
→ Apply
```

### 5.1.4 岗位发现

V1 至少支持职位关键词、城市、薪资、工作经验、发布时间、JD、招聘方活跃状态、Company、Job URL、来源平台，并跑通 **1～2 个真实来源**。

### 5.1.5 Job Normalize

最低字段：

```text
Job
├── title
├── company
├── city
├── salary
├── experience
├── education
├── jd
├── publish_time
├── recruiter_activity
├── source
└── source_url
```

### 5.1.6 Hard Filter

基础条件由确定性 Rule 处理，例如城市、薪资、经验和目标 Role。不满足硬条件的 Job 不进入后续分析。

### 5.1.7 JD Analysis

Broad Apply 只提取核心技术栈、主要职责、Must-have Requirement、经验要求和基础匹配度。

Targeted Apply 进行完整 Requirement Analysis，包括 Technical / Engineering / Business / Experience Requirement、Must Have / Important / Nice to Have、Expected Evidence、Capability Depth。

### 5.1.8 Job Ranking

Broad Apply 重点考虑：`Basic Match + Freshness + Recruiter Activity + Salary Fit - Application Cost`。

Targeted Apply 重点考虑：`Deep JD Match + Career Goal Fit + Company Value + Salary Value + Experience Leverage + Upgrade Feasibility`。

### 5.1.9 Resume Strategy

```text
Master Career Profile
        ↓
Role Resume
        ↓
Job-specific Resume
```

Broad Apply 优先使用 Role Resume；Targeted Apply 使用 Job-specific Resume。

### 5.1.10 模式切换

```text
BROAD_APPLY
      ↓
发现高价值岗位
      ↓
Promote
      ↓
TARGETED_APPLY
```

最终决定权由用户保留。

### 5.1.11 Job Market Feedback

Broad Apply 同时承担 **Job Market Experimentation**：逐渐识别哪些 Role 更匹配、哪些 Resume Strategy 更有效、哪些能力最值得优先补齐。

### 5.1.12 V1 范围与验收

V1 必须实现至少一个真实 Job 来源、Job Normalize、Hard Filter、两种策略、Fast / Deep JD Analysis、两套 Ranking Strategy、Role Resume / Job-specific Resume 路由、岗位模式升级和推荐理由。

验收重点：对候选岗位完成过滤、策略选择、排序、解释，并支持从 Broad 升级到 Targeted。

---

## 5.2 JD & Experience Match

### 5.2.1 目标

负责将岗位 Requirement 与用户已有 Experience、Project、Skill 建立匹配关系，并识别影响投递成功率的关键 Gap。

```text
BROAD_APPLY
→ Lightweight Match

TARGETED_APPLY
→ Deep Experience Match
```

### 5.2.2 Broad Apply：Lightweight Match

目标：快速判断岗位是否达到“值得投”的最低匹配标准。

输出 Basic Match Score、Matched Capabilities、Critical Gaps、Apply / Skip Recommendation，不默认触发深度 Experience Mining、Project Upgrade、Evidence 补全或 Job-specific Resume。

### 5.2.3 Targeted Apply：Deep Experience Match

```text
Requirement Analysis
→ Experience Retrieval
→ Experience Match
→ Gap Analysis
→ Upgrade Feasibility
→ Recommended Action
```

### 5.2.4 Match Status

| 状态 | 含义 |
|---|---|
| **MATCHED** | 当前经历已经能够满足要求 |
| **PARTIAL_MATCH** | 有相关能力，但深度不足 |
| **UPGRADABLE** | 可通过已有 Project 合理升级补齐 |
| **NOT_MATCHED** | 当前不存在合理匹配能力 |
| **UNKNOWN** | 信息不足，需要继续询问用户 |

### 5.2.5 Gap Analysis

- **Story Gap**：已做过但没讲清楚 → Experience Mining。
- **Skill Gap**：当前真实不存在 → Project Upgrade / Learning。
- **Evidence Gap**：能力可能存在但缺少证明 → Evidence Collection。

### 5.2.6 Upgrade Feasibility

判断：`HIGH / MEDIUM / LOW / NOT_APPLICABLE`。多年管理经验、真实生产用户规模、行业从业年限等不得通过 Project Upgrade 模拟。

### 5.2.7 V1 范围与验收

实现 Lightweight / Deep Match、Requirement → Experience 匹配、Match Status、三类 Gap、Upgrade Feasibility、Match Summary、Broad → Targeted 升级建议。

核心验收：能区分“已经会但没讲好”“可以补齐”“当前确实不具备”。

---

## 5.3 Experience Builder

### 5.3.1 目标

基于目标 Job 和用户真实经历，发现可挖掘信息、识别能力差距，并将已有 Experience 转化为更完整、更有深度、可验证的职业资产。

```text
Target Job + Existing Experience
→ Experience Mining
→ Gap Clarification
→ Story Enhancement
→ Project Upgrade（必要时）
→ Claim + Evidence
```

> **先挖掘真实经历，再判断是否需要真实补齐能力，而不是直接改写简历。**

### 5.3.2 Experience Mining

重点挖掘：`Business Context / Problem / Goal / Technical Decision / Implementation / Trade-off / Result / Retrospective`。

问题根据当前 Requirement 和信息缺口动态生成，不重复询问已有信息。

### 5.3.3 Story Enhancement

形成：`Context → Problem → Decision → Implementation → Result → Trade-off`。

### 5.3.4 Gap Routing

```text
Story Gap    → Experience Mining
Skill Gap    → Project Upgrade / Learning
Evidence Gap → Evidence Building
UNKNOWN      → Ask User → Re-evaluate
```

### 5.3.5 Project Upgrade Trigger

只有当 Gap 与目标 Job 高度相关、现有 Project 可合理扩展、成本可接受、完成后能产生 Evidence 且能明显提升岗位匹配度时，才建议 Project Upgrade。

### 5.3.6 Experience Upgrade Plan

至少包含 Target Requirement、Current Capability、Gap、Upgrade Goal、Expected Deliverables、Expected Evidence、Priority。

### 5.3.7 Experience Truth Boundary

必须区分 Past Experience、Upgraded Capability、Designed Scenario，并始终保持：`Project Upgrade ≠ Past Work Experience`。

### 5.3.8 V1 范围与验收

实现基于 JD 的 Experience Mining、动态追问、Story Gap 补全、Skill / Evidence Gap 路由、Project Upgrade 触发判断、Upgrade Plan、Truth Boundary 和结构化 Experience Asset。

核心验收：能把“真实但零散的经历”转化为“真实、深入、可验证的职业资产”。

---

## 5.4 Project Upgrade

### 5.4.1 目标

将可补齐 Skill Gap 转化为真实项目工程改造。

```text
Upgrade Plan
→ Human Approval
→ Codex Execution
→ Test / Eval
→ Evidence
→ Capability Update
```

> **只有真实完成并经过验证的能力，才能进入后续 Resume 和 Interview Story。**

### 5.4.2 Upgrade Plan

至少包含 Target Requirement、Current Project、Skill Gap、Upgrade Goal、Upgrade Scope、Expected Deliverables、Validation Method、Expected Evidence。

### 5.4.3 Codex Execution

Codex 负责 Repo 分析、代码修改、测试、Evaluation、技术文档和执行结果。RoleOS 负责为什么升级、升级目标、完成标准和 Evidence。

### 5.4.4 Validation 与 Evidence

Project Upgrade 不能以“代码已经生成”作为完成条件。应执行 Build / Test / Evaluation / Demo / Trace / Manual Verification 等验证。

成功后沉淀 SOURCE_CODE、GIT_COMMIT、TEST、EVALUATION、TRACE、ARCHITECTURE_DOCUMENT、DEMO 等 Evidence。

### 5.4.5 Upgrade Result

```text
PLANNED
APPROVED
IN_PROGRESS
VALIDATING
COMPLETED
PARTIAL
FAILED
```

只有验证完成的部分才能进入 Capability 和 Claim。

### 5.4.6 Truth Boundary

必须保持：`Upgraded Capability ≠ Past Production Experience`。

### 5.4.7 V1 范围与验收

实现 Upgrade Plan、Human Approval、Codex 真实 Repo 修改、Test / Eval、Upgrade 状态管理、Evidence 生成、Capability 更新、Partial / Failed 处理。

核心验收：能把“岗位能力缺口”转化为“可验证的新能力”。

---

## 5.5 Claim-Evidence & Resume Compiler

### 5.5.1 目标

将 Experience、Project Upgrade、Evidence、Target Job 转化为可用于简历、Portfolio 和面试的职业表达。

> **先确认能说什么，再决定怎么说。**

### 5.5.2 Claim Generation & Validation

候选 Claim 必须来源于用户确认的真实 Experience、已完成的 Project Upgrade 或可验证 Evidence。

状态：`VERIFIED / SUPPORTED / NEEDS_CONFIRMATION / UNSUPPORTED`。`UNSUPPORTED` 不得进入最终 Resume。

### 5.5.3 Resume Strategy

Broad Apply 使用 Role Resume；Targeted Apply 使用 Job-specific Resume。

```text
Master Career Profile
→ Role Resume
→ Job-specific Resume
```

### 5.5.4 Claim-Evidence Trace

至少支持从 Resume Claim 找到“这句话为什么可以写”。

### 5.5.5 Interview Consistency

Resume 中的重要 Claim 同步生成 Interview Story。

> **简历写到什么深度，用户就应该能够解释到什么深度。**

### 5.5.6 V1 范围与验收

实现 Claim Generation、Claim Validation、Claim-Evidence Mapping、Role Resume、Job-specific Resume、JD Requirement Coverage、Interview Story、Human Review、Resume Version 管理。

---

## 5.6 Application & Feedback

### 5.6.1 目标

```text
Resume Version
→ Human Approval
→ Application
→ Outcome
→ Feedback
→ Career Memory
```

> **让每一次投递都成为下一轮求职决策的反馈信号。**

### 5.6.2 Application Preparation

Broad Apply 使用 Role Resume + 低成本审核 + 批量候选池；Targeted Apply 使用 Job-specific Resume + Interview Story + 完整岗位分析 + 单岗位确认。

### 5.6.3 Human Approval

正式对外提交前必须获得用户确认。验证码、人机验证、信息不完整、敏感信息确认、平台异常、非预期外部操作时应强制暂停。

### 5.6.4 Application 状态

```text
PREPARED
WAITING_APPROVAL
SUBMITTED
VIEWED
REPLIED
INTERVIEWING
REJECTED
OFFER
WITHDRAWN
```

### 5.6.5 Broad / Targeted Feedback

Broad Apply 用于 Job Market Experimentation；Targeted Apply 更关注面试问题、失败原因、能力缺口和 Claim 是否经得住追问。

### 5.6.6 Career Feedback

Outcome 不直接修改 Career Profile 的事实，应先形成 Observation / Hypothesis / Recommendation，再逐步影响 Target Role、Job Ranking、Resume Strategy、Experience Builder、Project Upgrade Priority。

### 5.6.7 V1 范围与验收

实现 Application Preparation、Human Approval、状态管理、Broad / Targeted Strategy 记录、Outcome Tracking、投递指标、Interview Feedback、Career Feedback、Career Memory 更新。

---

# 6. 扩展功能

扩展功能不作为 V1 核心闭环的阻塞项。

| 优先级 | 定义 |
|---|---|
| **P1** | V1 核心闭环稳定后优先建设 |
| **P2** | 产品能力成熟后逐步扩展 |

## 6.1 Multi-site Job Discovery — P1

多招聘平台接入、跨平台 Job 去重、同一职位不同来源合并、Source 可用性管理、平台差异适配。

## 6.2 Advanced Browser Automation — P1

增强 Search、Navigate、Extract、Fill、Upload、Resume Session、Screenshot，并支持不同 Browser Runtime 切换。

## 6.3 Application Assistant — P1

自动选择 Resume、填写重复字段、上传附件、生成求职沟通内容、保存投递进度、恢复中断流程，但仍遵循 `Prepare Automatically → Human Approval → Submit`。

## 6.4 Interview Intelligence — P1

将 Interview Questions、User Recall、Job Requirement、Interview Result 转化为新的 Experience / Skill / Evidence Gap。

## 6.5 Career Intelligence — P1

分析哪些 Role 回复率更高、哪些 Skill 高频出现、哪些 Gap 最影响竞争力、下一阶段最值得升级什么项目。

## 6.6 Job Market Intelligence — P2

分析 Role Trend、Skill Trend、Salary Trend、Technology Trend。

## 6.7 Multi-Project Experience Builder — P2

自动选择和组合多个 Project 证明当前 Requirement。

## 6.8 Career Knowledge Graph — P2

长期建立 Role ↔ Requirement ↔ Capability ↔ Skill ↔ Experience ↔ Project ↔ Claim ↔ Evidence 关系。

## 6.9 Multi-Role Career Strategy — P2

支持多个职业方向的 Match、Opportunity、Gap、Upgrade Cost、Expected Return 分析。

## 6.10 Personalized Learning Loop — P2

```text
Skill Gap
→ Learning Task
→ Practice
→ Project
→ Evaluation
→ Evidence
→ Capability Update
```

## 6.11 范围原则

> **Core Loop First，Automation Second，Intelligence Later。**

---

# 7. 长期方向

RoleOS 的长期目标是演进为：

> **面向个人职业发展的 Agentic Career Operating System。**

## 7.1 阶段一：AI Career Agent

```text
Job Discovery
→ Job Match
→ Experience Builder
→ Project Upgrade
→ Resume
→ Application
→ Feedback
```

## 7.2 阶段二：Career Intelligence

逐步具备持续职业决策能力：判断更适合的 Role、市场正在需要什么、核心竞争力是什么、最值得补齐的 Skill 和 Project。

## 7.3 阶段三：Career Operating System

```text
Market
→ Role
→ Skill
→ Learning
→ Project
→ Experience
→ Job
→ Application
→ Interview
→ Outcome
→ Career Growth
↺
```

## 7.4 长期产品边界

长期仍坚持：不伪造职业经历、不用自动化代替关键职业决策、不以无差别批量投递作为核心价值、不重复建设成熟的 Browser / Coding Agent 基础设施、用户始终拥有 Career Data 和关键行动最终控制权。

---

# 8. 非功能需求

RoleOS 的非功能需求重点关注：`Reliability / Recoverability / Security / Explainability / Cost / Extensibility / Observability`。

## 8.1 可靠性

单个外部工具失败不能导致整个 Career Workflow 丢失。

> **失败可以发生，但状态不能丢失，事实不能被错误更新。**

## 8.2 可暂停与恢复

支持：

```text
RUNNING
→ WAITING_USER
→ PAUSED
→ RESUMED
→ COMPLETED
```

典型暂停场景包括 Experience Mining、Project Upgrade Approval、验证码或人工操作、Resume Review、用户隔天继续处理同一 Target Job。

## 8.3 安全与隐私

- 区分用户数据与公共 Job 数据；
- 敏感信息避免进入不必要的 Agent Context；
- 凭证、Token、Cookie 不以明文长期保存；
- 对外提交前保留 Human Approval；
- 关键数据修改可追踪。

## 8.4 可解释性

重要决策必须能够回答：为什么推荐这个 Job、为什么判定为 Targeted Apply、为什么存在某个 Gap、为什么升级这个 Project、为什么某条 Claim 可以进入 Resume。

## 8.5 成本控制

> **低价值任务使用低成本链路，高价值任务才进入深度链路。**

```text
Broad Apply   → Lightweight Analysis
Targeted Apply → Deep Analysis → Experience Mining → Project Upgrade
```

## 8.6 可扩展性

Job Source、Browser Provider、LLM Provider、Coding Agent、Evaluation Tool、Resume Renderer 均应可扩展，核心 Career Domain 不绑定单一外部实现。

## 8.7 可观测性

关键 Workflow 应追踪 Current Stage、Agent Decision、Tool Call、Human Approval、Codex Execution、Validation Result、Error、Token / Cost、Outcome。

## 8.8 数据一致性

必须区分：`User Fact / Agent Inference / Designed Scenario / Generated Content / Verified Evidence`。Agent 推理结果不能自动覆盖用户事实。

## 8.9 性能要求

V1 不追求高并发，重点是单用户完整 Career Workflow 是否稳定、可持续运行。

## 8.10 V1 非功能验收

- [ ] Career Workflow 可以暂停和恢复；
- [ ] 外部工具失败不会导致核心状态丢失；
- [ ] 关键外部操作存在 Human Approval；
- [ ] 用户事实与 Agent 推理明确区分；
- [ ] Job Recommendation 和 Gap Decision 可解释；
- [ ] Claim 能追溯到 Experience / Evidence；
- [ ] Broad / Targeted 使用不同成本策略；
- [ ] Browser、Codex 等外部能力可替换；
- [ ] 关键 Workflow 具备基础 Trace；
- [ ] 敏感凭证不以明文业务数据保存。

---

# 9. 关键业务流程

RoleOS V1 重点定义 Broad Apply、Targeted Apply、Project Upgrade、Application Feedback 四条核心流程。

## 9.1 Broad Apply 流程

```text
用户设置 Target Role / 城市 / 薪资
→ Job Discovery
→ Job Normalize
→ Hard Filter
→ Fast JD Analysis
→ Lightweight Experience Match
→ Broad Apply Ranking
→ Role Resume Selection
→ Human Batch Review
→ Application
→ Outcome Tracking
```

关键规则：不对每个岗位执行完整 Experience Mining，不默认触发 Project Upgrade，使用 Role Resume，高价值岗位可升级为 Targeted Apply。

## 9.2 Targeted Apply 流程

```text
选择 Target Job
→ Deep JD Analysis
→ Experience Retrieval
→ Deep Experience Match
→ Gap Analysis
→ Experience Mining
→ 是否需要 Project Upgrade？
→ Claim-Evidence Validation
→ Job-specific Resume
→ Interview Story
→ Human Approval
→ Application
→ Outcome Tracking
```

## 9.3 Project Upgrade 流程

```text
Skill Gap
→ Upgrade Feasibility
→ Upgrade Plan
→ Human Approval
→ Codex Execution
→ Repo Change
→ Test / Evaluation
→ Validation Result
→ Evidence Generation
→ Capability Update
```

结果：`COMPLETED / PARTIAL / FAILED`。

## 9.4 Application Feedback 流程

```text
Application
→ Outcome
→ Feedback Collection
→ Career Feedback
→ Career Memory
→ 更新 Job Ranking / Resume Strategy / Experience Strategy / Project Upgrade Priority
```

## 9.5 Broad → Targeted 升级流程

```text
Broad Candidate Job
→ 高匹配 / 高价值 / 高可提升空间
→ Recommend Promotion
→ Human Approval
→ TARGETED_APPLY
→ Deep Experience Match
```

## 9.6 Workflow 暂停点

Experience Mining、Project Upgrade Approval、Browser Human Verification、Resume Review、Application Approval。

## 9.7 核心状态流转

```text
DISCOVERED
→ FILTERED
→ ANALYZED
→ MATCHED
→ BUILDING_EXPERIENCE
→ READY_TO_APPLY
→ SUBMITTED
→ OUTCOME_RECEIVED
→ COMPLETED
```

提前结束状态：`FILTERED_OUT / SKIPPED / REJECTED_BY_USER / UPGRADE_FAILED`。

## 9.8 V1 流程验收

Broad Apply：`真实岗位 → 快速筛选 → Role Resume → Human Review → 投递记录`。

Targeted Apply：`真实 JD → Experience Match → Gap Analysis → Experience Builder → Resume → Human Approval`。

Project Upgrade：`Skill Gap → Upgrade Plan → Codex → Test / Eval → Evidence`。

Feedback Loop：`Application → Outcome → Career Feedback → 下一轮策略调整`。

---

# 10. Domain Model

RoleOS V1 的领域模型围绕 Job、Experience、Upgrade、Application 四条主线组织。

## 10.1 CareerProfile

```text
CareerProfile
├── Target Roles
├── Job Preferences
├── Experiences
├── Projects
├── Skills
├── Capabilities
└── Career Strategy
```

## 10.2 Role

表示用户希望申请的一类职业角色，用于 Job 分类、Role Resume、Career Strategy、Job Market Analysis。

## 10.3 Job

```text
Job
├── Company
├── Title
├── Salary
├── City
├── JD
├── Publish Time
├── Recruiter Activity
├── Source
├── Strategy
└── Status
```

Strategy：`BROAD_APPLY / TARGETED_APPLY`。

## 10.4 Requirement

```text
Requirement
├── Capability
├── Type
├── Priority
├── Expected Depth
└── Expected Evidence
```

## 10.5 Experience

表示用户真实发生过的职业经历，包括 Work Experience、Project Experience、Personal Project、Open-source Experience、Learning Experience。

## 10.6 Project

```text
Project
├── Experience
├── Skills
├── Capabilities
├── Claims
├── Evidence
└── Upgrades
```

## 10.7 ExperienceMatch

```text
ExperienceMatch
├── Requirement
├── Experience / Project
├── Match Status
├── Match Score
└── Reason
```

Match Status：`MATCHED / PARTIAL_MATCH / UPGRADABLE / NOT_MATCHED / UNKNOWN`。

## 10.8 Gap

类型：`STORY_GAP / SKILL_GAP / EVIDENCE_GAP`。

## 10.9 Upgrade

```text
Upgrade
├── Project
├── Target Requirements
├── Upgrade Plan
├── Status
├── Validation
└── Evidence
```

状态：`PLANNED / APPROVED / IN_PROGRESS / VALIDATING / COMPLETED / PARTIAL / FAILED`。

## 10.10 Claim

```text
Claim
├── Experience / Project
├── Requirement
├── Evidence
└── Resume Version
```

## 10.11 Evidence

类型：`USER_STATEMENT / SOURCE_CODE / GIT_COMMIT / TEST / EVALUATION / TRACE / ARCHITECTURE_DOCUMENT / DEMO`。

## 10.12 ResumeVersion

```text
ResumeVersion
├── Target Role / Job
├── Resume Type
├── Selected Experiences
├── Claims
├── Created Time
└── Review Status
```

Resume Type：`ROLE_RESUME / JOB_SPECIFIC_RESUME`。

## 10.13 Application

```text
Application
├── Job
├── Resume Version
├── Strategy
├── Apply Time
├── Channel
└── Status
```

## 10.14 Outcome

表示 Application 后产生的真实结果，例如 VIEWED、REPLIED、INTERVIEW、REJECTED、OFFER。

## 10.15 CareerWorkflow

```text
CareerWorkflow
├── Type
├── Target
├── Current Stage
├── Status
├── Context
└── Human Approval
```

典型 Workflow Type：`BROAD_APPLY / TARGETED_APPLY / PROJECT_UPGRADE`。

## 10.16 HumanApproval

典型类型：`EXPERIENCE_CONFIRMATION / PROJECT_UPGRADE_APPROVAL / RESUME_APPROVAL / APPLICATION_APPROVAL`。

结果：`APPROVED / REJECTED / EDIT_REQUESTED / DEFERRED`。

## 10.17 核心关系总结

```text
Role
 ↓
Job
 ↓
Requirement
 ↓
ExperienceMatch
 ↙          ↘
Experience   Gap
   ↓          ↓
Project    Upgrade
   │          ↓
   ├───── Evidence
   │          ↓
   └────── Claim
              ↓
        ResumeVersion
              ↓
         Application
              ↓
           Outcome
              ↓
       Career Feedback
              ↓
        CareerProfile
```

V1 原则：**优先支撑核心业务闭环，不提前构建完整 Career Knowledge Graph。**

---

# 11. 里程碑规划

RoleOS V1 计划在 **6～8 周**内完成。

> **每个阶段都必须形成可运行、可验证、可演示的结果。**

## 11.1 Week 1：Career Domain 基础

完成 Career Profile、Role / Job / Requirement、Experience / Project、Career Workflow、基础数据持久化。

## 11.2 Week 2：Job Intelligence

完成 Job Source、Job Normalize、Hard Filter、Broad / Targeted Strategy、Fast JD Analysis、Job Ranking。

## 11.3 Week 3：JD & Experience Match

完成 Experience Retrieval、Lightweight Match、Deep Match、Match Status、Story / Skill / Evidence Gap、Upgrade Feasibility。

## 11.4 Week 4：Experience Builder

完成 Experience Mining、动态追问、Story Enhancement、Gap Routing、Upgrade Plan、Experience Truth Boundary。

## 11.5 Week 5：Project Upgrade + Codex

完成 Human Approval、Codex Task、Repo 修改、Test / Evaluation、Upgrade 状态管理、Evidence 生成。

## 11.6 Week 6：Claim-Evidence + Resume

完成 Claim Generation、Claim Validation、Claim-Evidence Mapping、Role Resume、Job-specific Resume、Interview Story。

## 11.7 Week 7：Application & Feedback

完成 Application Preparation、Human Approval、Application 状态、Outcome Tracking、Broad / Targeted 指标、Career Feedback。

## 11.8 Week 8：集成与 V1 验收

重点处理 Workflow 集成、Pause / Resume、错误恢复、Trace、成本优化、Prompt / Agent 行为优化、Bug Fix。

最终演示：

```text
真实 Job
→ JD Analysis
→ Experience Match
→ Experience Mining
→ Project Upgrade
→ Evidence
→ Resume
→ Interview Story
→ Application
→ Outcome
```

## 11.9 V1 核心里程碑

| 阶段 | 核心结果 |
|---|---|
| M1 | Career Domain + JD 可以结构化 |
| M2 | 能发现、筛选、排序真实 Job |
| M3 | 能完成 Requirement → Experience → Gap |
| M4 | 能通过 Experience Mining 深挖真实经历 |
| M5 | Codex 能真实升级 Project 并产生 Evidence |
| M6 | Evidence 能生成可信 Resume / Interview Story |
| M7 | 能完成 Application + Feedback |
| **M8** | **完整 Golden Scenario 稳定跑通** |

## 11.10 V1 完成条件

> **一个真实用户、一个真实岗位、一个真实项目能够完整跑通 RoleOS 核心闭环。**

---

# 12. 风险与未决事项

## 12.1 核心风险

### R1. 招聘平台自动化限制

可能存在登录限制、验证码、反自动化机制、页面结构变化和平台使用条款限制。原则：Browser 可替换、失败允许人工接管、不绕过验证码、不依赖大规模无人值守抓取和投递。

### R2. Broad Apply 成本失控

坚持 `Broad Apply → Lightweight Analysis`，`Targeted Apply → Deep Analysis`。

### R3. Job Ranking 有分数但没有真实价值

Ranking 必须通过真实 Outcome 持续验证：`Ranking → Application → Reply / Interview → Feedback`。

### R4. Experience 真实性被 Agent 污染

严格区分：`User Fact / Agent Inference / Designed Scenario / Upgraded Capability / Verified Evidence`。

### R5. Experience Mining 过度追问

目标不是获得最完整信息，而是获得支持当前求职决策所需的最少有效信息。

### R6. Project Upgrade 投入大于求职收益

优先建设可以复用于多个目标岗位的 Capability。

### R7. Codex 执行结果不可控

`Codex Output ≠ Upgrade Completed`，必须经过 Validation → Evidence → Capability Update。

### R8. Resume 优化过度

优先级：`Truth > Relevance > Keyword Coverage`。

### R9. V1 范围膨胀

坚持 **Golden Scenario First**。

### R10. OryxOS 二开耦合过深

RoleOS Career Domain 应保持独立，不依赖 OryxOS 内部实现细节。

## 12.2 未决事项

### D1. V1 首个 Job Source

根据岗位质量、实际使用频率、自动化可行性、页面稳定性选择。

### D2. Broad Apply 的自动化程度

建议 V1 采用“自动发现 + 人工确认投递”或“自动准备 + 批量确认”，正式提交保留 Human Approval。

### D3. Job Ranking 权重

V1 先使用可解释规则，不提前建设复杂 Ranking Model。

### D4. Experience Mining 结束条件

技术设计阶段制定最小信息标准，防止无限追问。

### D5. Project Upgrade 优先级算法

综合 Requirement Priority、Gap Reusability、Upgrade Cost、Evidence Value、Career Goal。

### D6. Evidence 强度标准

V1 需要定义最小 Claim-Evidence Validation 规则。

### D7. Career Memory 的保存边界

明确长期 Career Fact、Workflow 临时 Context 和值得长期保存的 Agent 推理。

## 12.3 V1 风险控制原则

```text
真实优先
闭环优先
人工可控
失败可恢复
成本可控制
复杂度后置
```

---

# 13. 验收标准

RoleOS V1 验收分为：`模块验收 → 端到端场景验收 → V1 发布验收`。

## 13.1 核心模块验收

| 模块 | 验收标准 |
|---|---|
| **Job Intelligence** | 能获取真实 Job，完成 Normalize、Filter、Broad / Targeted 分类和排序 |
| **JD & Experience Match** | 能识别 Requirement，并区分 Matched、Story Gap、Skill Gap、Evidence Gap |
| **Experience Builder** | 能针对目标 JD 深挖真实经历，并生成结构化 Experience Asset |
| **Project Upgrade** | 能生成 Upgrade Plan，调用 Codex 修改真实 Repo，并通过 Test / Eval 验证 |
| **Claim-Evidence** | 关键 Claim 能关联真实 Experience 或 Evidence |
| **Resume Compiler** | Broad 生成 Role Resume，Targeted 生成 Job-specific Resume |
| **Application** | 正式投递前存在 Human Approval，并能记录 Application 状态 |
| **Feedback** | 能记录 Outcome，并形成下一轮 Career Feedback |

## 13.2 Broad Apply 场景验收

```text
Job Discovery
→ Hard Filter
→ Fast JD Analysis
→ Lightweight Match
→ Broad Ranking
→ Role Resume
→ Human Review
→ Application Record
```

## 13.3 Targeted Apply 场景验收

```text
Deep JD Analysis
→ Experience Match
→ Gap Analysis
→ Experience Mining
→ Project Upgrade（如需要）
→ Evidence
→ Job-specific Resume
→ Interview Story
→ Human Approval
```

## 13.4 Project Upgrade 场景验收

```text
Codex Execution
→ Repo Change
→ Test / Eval
→ Validation
→ Evidence
→ Capability Update
```

必须支持 COMPLETED / PARTIAL / FAILED，失败部分不能进入 Claim，新能力不得反向写成过去 Work Experience。

## 13.5 Claim-Evidence 验收

重要 Claim 应至少能够追溯到 User Confirmed Experience、Source Code、Git Commit、Test、Evaluation、Trace、Architecture Document 或 Demo。

不得进入 Verified Claim：未真实发生的客户、未验证生产规模、虚构用户数或收入、未执行过的性能指标、Agent 自行推测的历史经历。

## 13.6 非功能验收

Workflow 可暂停恢复；外部工具失败不丢核心状态；关键动作存在 Human Approval；用户事实与 Agent 推理明确区分；关键 Decision 可解释；流程具备基础 Trace；Broad / Targeted 使用不同成本策略；Browser / Codex 不与 Career Domain 强绑定。

## 13.7 Golden Scenario

V1 发布前必须完整演示：

```text
真实 AI 岗位
→ Job Intelligence
→ JD Requirement
→ Experience Match
→ Gap Analysis
→ Experience Mining
→ Project Upgrade
→ Codex 修改真实 Repo
→ Test / Eval
→ Evidence
→ Job-specific Resume
→ Interview Story
→ Human Approval
→ Application
→ Outcome / Feedback
```

要求使用真实 JD、真实 Career Profile、真实 Project Repo，Project Upgrade 必须产生实际代码修改，Claim 符合真实性边界，Workflow 中断后可继续执行。

## 13.8 V1 发布硬条件

- [ ] Broad Apply 核心流程可运行；
- [ ] Targeted Apply 核心流程可运行；
- [ ] Experience Builder 能完成真实经历深挖；
- [ ] Codex 能完成至少一次真实 Project Upgrade；
- [ ] Upgrade 能产生有效 Evidence；
- [ ] Resume 中关键 Claim 可追溯；
- [ ] Human Approval 生效；
- [ ] Application / Outcome 可以记录；
- [ ] Workflow 支持暂停和恢复；
- [ ] Golden Scenario 完整跑通。

---

# 14. 总结

RoleOS 是一套面向 **AI 应用开发工程师、AI Agent 开发工程师及相关技术岗位求职者** 的 AI Career Agent。

它围绕目标岗位，将：

```text
Job
→ Experience
→ Gap
→ Upgrade
→ Evidence
→ Resume
→ Application
→ Feedback
```

连接成完整闭环。

RoleOS 的核心价值不是抓取更多岗位、生成更多简历、完成更多投递，而是：

> **帮助用户找到值得投入的岗位，并通过真实经历深挖和项目升级，持续提升与目标岗位的真实匹配度。**

## 14.1 V1 核心能力

```text
Job Intelligence
→ JD & Experience Match
→ Experience Builder
→ Project Upgrade
→ Claim-Evidence & Resume
→ Application & Feedback
```

其中最核心的差异化能力是：**Experience Builder + Project Upgrade + Claim-Evidence**。

## 14.2 两种求职策略

- **BROAD_APPLY**：低成本扩大有效机会覆盖。
- **TARGETED_APPLY**：高价值岗位深度定制，提高单岗位竞争力。

## 14.3 核心产品边界

必须始终保持：`Portfolio Project ≠ Past Production Experience`，`Project Upgrade ≠ 过去真实工作经历`。

## 14.4 V1 最终目标

RoleOS V1 计划在 **6～8 周**内完成，并完整跑通：

```text
真实 Job
→ JD Analysis
→ Experience Match
→ Gap Analysis
→ Experience Mining
→ Project Upgrade
→ Codex
→ Test / Eval
→ Evidence
→ Resume
→ Interview Story
→ Application
→ Outcome
```

只有当这条链路能够稳定、可信、可恢复地运行时，才能认为：

> **RoleOS V1 核心产品假设成立。**
