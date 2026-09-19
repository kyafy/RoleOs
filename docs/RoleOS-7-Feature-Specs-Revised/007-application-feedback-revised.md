# 007 — Application & Feedback

> **Feature ID**：007  
> **Feature Name**：Application & Feedback  
> **Product**：RoleOS  
> **Stage**：V1  
> **Status**：Revised after Cross-Feature Consistency Review

---

# 1. 目标

Application & Feedback 是 RoleOS V1 的求职闭环执行层。

它承接：

```text
Target Job
+
Approved Resume
+
Application Strategy
```

并推进到：

```text
Application Preparation
→ Human Approval
→ Safe Submission
→ Outcome Tracking
→ Feedback Loop
```

它解决的问题是：

```text
这份岗位到底要不要投？
应该用哪份 Resume？
提交前还缺什么信息？
如何避免重复投递？
投递后发生了什么？
这些结果应该如何反向影响后续策略？
```

核心原则：

> **Application 是有外部副作用的真实操作，必须优先保证可控、可追踪、不可重复误提交。**

---

# 2. 范围

007 负责：

- Application Preparation；
- Broad / Targeted Application Strategy；
- Resume Version 绑定；
- Required Information 检查；
- Human Approval；
- Assisted Submission；
- Browser Submission；
- Submission Idempotency；
- SUBMISSION_UNKNOWN；
- Human Takeover；
- Outcome Tracking；
- 自动 + 手动 Outcome；
- Funnel Metrics；
- Observation / Hypothesis / Recommendation；
- Application Dashboard。

本 Feature 不负责：

- 新 Job Discovery；
- Experience Mining；
- Project Upgrade；
- Resume Compiler；
- 自动修改 Career Fact；
- 绕过 Captcha / Human Verification；
- 无人工确认的不可逆提交。

---

# 3. 核心 User Stories

## US-007-01 — 准备投递

作为用户，我希望 RoleOS 能基于 Target Job 和已批准 Resume 自动准备 Application，让我在提交前明确知道要投什么、用哪份 Resume、还缺什么信息。

## US-007-02 — 安全提交

作为用户，我希望正式投递前必须由我确认，并且 Browser Retry、Provider Fallback 或服务重启不会导致重复提交。

## US-007-03 — 跟踪结果

作为用户，我希望 RoleOS 能跟踪岗位是否被查看、回复、进入面试、被拒或获得 Offer，同时允许我手工补充线下或跨平台结果。

## US-007-04 — 从结果中学习

作为用户，我希望 RoleOS 能根据真实 Outcome 形成 Observation、Hypothesis 和 Recommendation，帮助后续优化 Job Ranking、Resume、Experience Builder 和 Project Upgrade。

---

# 4. Application Model

Application 表示：

> **一次针对具体 Job、使用具体 Resume Version、通过具体 Channel 发起的求职动作。**

至少包含：

```text
Application
├── userId
├── jobId
├── resumeVersionId
├── strategySnapshot
├── channel
├── requiredInformation
├── currentStatus
├── submissionIdentity
├── preparedAt
├── submittedAt
├── lastOutcomeAt
└── metadata
```

必须保持：

```text
Job
+
Approved Resume Version
+
Application Strategy Snapshot
+
Channel
```

可追溯。

`strategySnapshot` 从创建 Application 时的 `JobCandidate.strategy` 复制并固定，用于保留历史投递决策。后续即使 JobCandidate 从 Broad 调整为 Targeted，也不能反向改写已经创建的 Application。

正式 Application 只能绑定：

```text
ResumeVersionStatus = APPROVED
```

---

# 5. Application Strategy

007 延续前面两种策略：

```text
BROAD_APPLY
TARGETED_APPLY
```

但在投递阶段使用不同成本模型。

---

## 5.1 Broad Apply

目标：

```text
低成本
高覆盖
快速 Review
```

通常使用：

```text
Role Resume
```

并允许：

```text
Batch Candidate Queue
Simplified Review
```

但正式 Submit 仍然遵守安全规则。

---

## 5.2 Targeted Apply

目标：

```text
高质量
高匹配
高投入
```

通常使用：

```text
Job-specific Resume
```

并进行更完整的：

```text
Application Review
Claim Review
Required Information Review
```

---

# 6. Assisted Application

V1 采用：

```text
Assisted Application
```

而不是追求完全无人值守投递。

基本流程：

```text
Prepare
→ Auto-fill Safe Fields
→ Review
→ Human Approval
→ Submit
```

RoleOS 可以自动处理：

- Job 信息整理；
- Resume 选择；
- 常规字段预填；
- 可确定信息填充；
- Submission 前检查。

但正式提交：

> **必须经过用户确认。**

---

# 7. Application Preparation

进入提交前，RoleOS 必须准备：

```text
Target Job
Selected Resume Version
Application Strategy
Channel
Required Fields
Optional Questions
Attachments
Contact Information
Submission Risks
```

如果缺少必要信息：

```text
Application.currentStatus = PREPARED
CareerWorkflow.status = WAITING_USER_INPUT
```

而不是强行填写。

---

# 8. Human Approval

Application 正式 Submit 前：

```text
Application.currentStatus = PREPARED
CareerWorkflow.status = WAITING_APPROVAL
```

该决策属于 `ApplicationDecision`，由 001 的通用 `HumanDecision` 机制承载。

用户至少可以：

```text
APPROVE
EDIT
SKIP
DEFER
```

---

## 8.1 APPROVE

允许进入 Submission。

---

## 8.2 EDIT

用户可以修改：

- Resume Version；
- Contact Information；
- Optional Answer；
- Attachment；
- Application Note。

如果修改涉及新的 Career Fact：

```text
不能自动成为已确认事实
```

---

## 8.3 SKIP

当前 Job 不再投递。

---

## 8.4 DEFER

暂不投递，但保留 Application。

---

# 9. Application Status

V1 使用领域状态：

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

以下状态属于 `CareerWorkflowStatus`，不属于 ApplicationStatus：

```text
WAITING_USER_INPUT
WAITING_APPROVAL
PAUSED_FOR_HUMAN
```

---

# 10. Submission Safety

正式 Submit 属于：

```text
Side-effect Operation
```

因此必须使用比普通读取操作更严格的规则。

核心原则：

```text
Side-effect Operation
≠
Automatically Retryable
```

---

# 11. Submission Idempotency

Browser Retry、Provider Fallback、Workflow Resume 都不得导致重复投递。

Application 至少需要基于：

```text
Job
+
Resume Version
+
Channel
+
Submission Identity
```

建立幂等保护。

---

## 11.1 已确认成功

如果有明确证据：

```text
提交成功提示
平台状态变化
消息已发送
Application Record Confirmed
```

则：

```text
SUBMITTED
```

---

## 11.2 明确失败

如果能够确认：

```text
Submit 未产生任何外部副作用
```

才可以进入可重试逻辑。

---

## 11.3 结果不确定

例如：

```text
点击 Submit
→ 页面超时
→ Browser 无响应
```

无法判断是否已经产生外部副作用。

必须：

```text
SUBMISSION_UNKNOWN
```

不得自动重试。

---

# 12. SUBMISSION_UNKNOWN

`SUBMISSION_UNKNOWN` 表示：

> **系统已经尝试产生投递副作用，但无法确认最终结果。**

进入该状态后：

```text
Application.currentStatus = SUBMISSION_UNKNOWN
CareerWorkflow.status = PAUSED_FOR_HUMAN
```

用户可以：

- 手工检查平台；
- 确认已提交；
- 确认未提交；
- 决定是否再次执行。

RoleOS 不能自行假设成功或失败。

---

# 13. Browser & Human Takeover

007 延续 BrowserProvider 架构：

```text
Application Workflow
→ JobSiteAdapter
→ BrowserRouter
→ BrowserProvider
```

但 Submission 阶段比 Job Discovery 更严格。

遇到：

```text
Captcha
Human Verification
Unexpected Confirmation
Sensitive Information
Platform Risk Warning
Unknown Side Effect
```

必须：

```text
CareerWorkflow.status = PAUSED_FOR_HUMAN
```

---

## 13.1 Provider Fallback

对于纯读取或明确无副作用的步骤：

```text
可以进行 Provider Fallback
```

对于已经可能触发 Submit 的步骤：

```text
不能因为 Provider Failure 直接在另一 Provider 再 Submit
```

必须先确认 Side Effect State。

---

# 14. Outcome Tracking

投递完成后，Application 进入 Outcome Tracking。

V1 支持：

```text
Automatic Observation
+
Manual Update
```

---

## 14.1 Automatic Observation

RoleOS 可以从招聘平台识别：

```text
VIEWED
REPLIED
INTERVIEWING
REJECTED
```

具体可获取状态取决于平台能力。

---

## 14.2 Manual Update

用户可以手工记录：

```text
HR 电话联系
微信沟通
线下面试
面试结果
Offer
Reject
Withdraw
```

因为真实求职过程可能离开原招聘平台。

---

# 15. Outcome Model

Outcome 应与：

```text
Job
Resume Version
Application Strategy
Application
```

保持关联。

可以记录：

```text
Outcome Type
Occurred Time
Source
Raw Observation
User Note
Interview Questions
Rejection Reason
```

Outcome 是观察结果，并作为 append-only event 保存。

它不是 Career Fact。

`Application.currentStatus` 是当前业务快照：

```text
Outcome Event(s)
→ Status Projection Rule
→ Application.currentStatus
```

例如：

```text
Outcome(VIEWED)
→ Application.currentStatus = VIEWED
```

不得让 Outcome 和 Application.currentStatus 各自独立成为两套可随意修改的 Source of Truth。

---

# 16. Feedback Loop

007 的核心闭环：

```text
Outcome
→ Observation
→ Hypothesis
→ Recommendation
→ User / Rule Decision
→ Action
```

必须保持：

> **Outcome 可以影响后续策略，但不能直接覆盖 Career Fact。**

---

## 16.1 Observation

Observation 是对结果的结构化总结。

例如：

```text
20 次 Broad Apply
15 次 Viewed
2 次 Reply
0 次 Interview
```

---

## 16.2 Hypothesis

Agent 可以提出解释：

```text
可能是 Resume 首屏 Claim 不够强
可能是岗位选择偏宽
可能是核心 Skill Coverage 不足
```

Hypothesis 是：

```text
推测
≠
事实
```

---

## 16.3 Recommendation

基于 Hypothesis，可以建议：

```text
调整 Job Ranking
修改 Role Resume
重新做 Experience Mining
增加 Project Upgrade
加强 Evidence
调整 Target Role
```

但不直接自动执行高影响修改。

---

# 17. Broad Apply Metrics

V1 Broad Funnel 至少统计：

```text
Applications
View Rate
Reply Rate
Interview Rate
Offer Rate
```

目的不是追求复杂数据分析。

而是帮助回答：

```text
岗位覆盖有没有问题？
Resume 是否能推动 Recruiter 回复？
Broad Strategy 是否有效？
```

---

# 18. Targeted Apply Feedback

Targeted Apply 除基础 Funnel 外，还应关联：

```text
Requirement Coverage
Resume Version
Interview Questions
Rejection Reason
Gap Signals
Claim Robustness
```

帮助分析：

```text
岗位匹配判断是否准确？
Job-specific Resume 是否有效？
哪些 Claim 在 Interview 中站得住？
哪些 Gap 仍然真实存在？
```

---

# 19. Claim Robustness

如果 Interview 中某个 Claim：

```text
可以被用户清晰解释
能说明 Context / Decision / Trade-off
有 Evidence 支撑
```

可以形成：

```text
Positive Claim Signal
```

如果用户发现：

```text
Claim 无法解释
事实支撑不足
```

则形成：

```text
Claim Weakness Observation
```

但：

```text
不能自动修改 Claim Status
```

需要后续 Review。

---

# 20. Application Dashboard

007 提供 Application Dashboard。

至少展示：

```text
Job
Company
Strategy
Resume Version
Application Status
Submitted Time
Latest Outcome
Next Action
```

---

## 20.1 Funnel View

至少显示：

```text
Applied
Viewed
Replied
Interviewing
Offer
Rejected
```

支持 Broad / Targeted 分开查看。

---

## 20.2 Targeted Application Detail

至少展示：

```text
Target Job
Requirements
Resume Version
Application Timeline
Interview Feedback
Rejection Reason
Gap Signals
Recommendations
```

---

# 21. Application Timeline

每个 Application 应有可追踪 Timeline，但 UI 必须区分 Domain Status 与 Workflow Status。

例如：

```text
Application = PREPARED
CareerWorkflow.status = WAITING_APPROVAL

→ User Approves

Application = SUBMITTING
Workflow = RUNNING

→ Application = SUBMITTED
→ Outcome(VIEWED)
→ Application = VIEWED
→ Outcome(REPLIED)
→ Application = REPLIED
```

异常链路：

```text
Application = SUBMITTING
→ Application = SUBMISSION_UNKNOWN
→ CareerWorkflow.status = PAUSED_FOR_HUMAN
→ Human Check
→ Application = SUBMITTED
```

Timeline 用于：

- 用户理解；
- Retry 判断；
- Feedback 分析；
- Audit。

---

# 22. Core Business Rules

```text
R-001
V1 Application = Assisted Application
```

不追求无人值守全自动提交。

```text
R-002
Formal Submit requires Human Approval
```

正式提交前必须确认。

```text
R-003
Broad and Targeted use different cost strategies
```

但都遵守安全边界。

```text
R-004
Submit is a side-effect operation
```

安全要求高于普通 Browser Read。

```text
R-005
Submission must be idempotent
```

Workflow Retry 不能导致重复投递。

```text
R-006
Unknown submission result → SUBMISSION_UNKNOWN
```

不得自动 Retry。

```text
R-007
Provider Fallback must respect side-effect state
```

不能跨 Provider 盲目重复 Submit。

```text
R-008
Captcha / Verification → Human Takeover
```

不绕过平台安全控制。

```text
R-009
Automatic + Manual Outcome are both valid
```

真实求职过程不局限于单个平台。

```text
R-010
Outcome ≠ Career Fact
```

结果不能直接修改用户职业事实。

```text
R-011
Observation → Hypothesis → Recommendation
```

Agent 可以推断，但不能把 Hypothesis 当事实。

```text
R-012
Feedback drives recommendation, not silent mutation
```

重要变化由用户或明确规则决定。

```text
R-013
CareerWorkflowStatus ≠ ApplicationStatus
```

等待输入、审批、人工接管属于 Workflow；PREPARED / SUBMITTED / VIEWED 等属于 Application。

```text
R-014
Outcome is append-only; Application.currentStatus is a projection
```

Outcome 保存真实观察事件，Application.currentStatus 根据有效 Outcome 和 Submission 状态派生。

```text
R-015
Application Strategy is a snapshot
```

创建 Application 后必须保留当时的 Broad / Targeted Strategy，不被后续 JobCandidate 决策覆盖。

---

# 23. Acceptance Scenarios

## AC-001 — Broad Application Preparation

Given：

```text
JobCandidate Strategy = BROAD_APPLY
Role ResumeVersion.status = APPROVED
```

When：

```text
用户准备投递
```

Then：

```text
Application = PREPARED
绑定 Job + Approved Resume Version + Strategy Snapshot + Channel
```

---

## AC-002 — Targeted Application

Given：

```text
JobCandidate Strategy = TARGETED_APPLY
Job-specific ResumeVersion.status = APPROVED
```

Then：

```text
Application 使用 Job-specific Resume
并进入更完整 Review
```

---

## AC-003 — Human Approval

Given：

```text
Application 已准备完成
```

Then：

```text
Application.currentStatus = PREPARED
CareerWorkflow.status = WAITING_APPROVAL
```

未经：

```text
APPROVE
```

不得正式 Submit。

---

## AC-004 — User Edit

在 Approval 页面：

```text
用户可以更换 Resume
修改附加信息
修改 Optional Answer
```

然后再次 Review。

---

## AC-005 — Successful Submission

Given：

```text
用户批准
Browser 执行 Submit
平台明确返回成功
```

Then：

```text
Status = SUBMITTED
记录 submittedAt
记录 Submission Evidence
```

---

## AC-006 — Submission Unknown

Given：

```text
Submit 已触发
但 Browser 在结果返回前超时
```

Then：

```text
Application.currentStatus = SUBMISSION_UNKNOWN
CareerWorkflow.status = PAUSED_FOR_HUMAN
```

系统不得自动再次 Submit。

---

## AC-007 — Browser Provider Failure

Given：

```text
Playwright 在 Submit 之后失败
```

Then：

```text
不能直接切到 Kimi 再 Submit
```

必须先判断：

```text
Side Effect 是否已发生
```

---

## AC-008 — Human Verification

当出现：

```text
Captcha / Human Verification / Risk Warning
```

Then：

```text
CareerWorkflow.status = PAUSED_FOR_HUMAN
```

用户完成后可以 Resume。

---

## AC-009 — Automatic Outcome

Given：

```text
平台显示 Recruiter 已查看
```

Then：

```text
Outcome(VIEWED) 被记录
Application.currentStatus = VIEWED
```

并形成 Outcome Observation。

---

## AC-010 — Manual Outcome

用户在线下收到电话面试：

```text
可以手工记录 INTERVIEWING
```

即使招聘平台没有对应状态。

---

## AC-011 — Feedback Loop

Given：

```text
20 次 Broad Apply
View Rate 高
Reply Rate 很低
```

Then：

RoleOS 可以生成：

```text
Observation
+
Hypothesis
+
Recommendation
```

但不得自动修改 Career Fact 或 Resume。

---

## AC-012 — Funnel Dashboard

用户可以看到：

```text
Applied
Viewed
Replied
Interviewing
Offer
Rejected
```

并区分：

```text
Broad
Targeted
```

---

# 24. Edge Cases

### Duplicate Submit Trigger

如果用户重复点击：

```text
Submit
```

系统必须通过 Submission Identity / Current Status 拦截重复副作用。

---

### Browser Timeout Before Click

如果能够明确：

```text
Submit 尚未执行
```

可以安全 Retry。

---

### Browser Timeout After Click

如果无法明确：

```text
是否提交成功
```

必须：

```text
SUBMISSION_UNKNOWN
```

---

### Platform Status Ambiguous

如果平台文案无法可靠映射：

```text
不要强行更新为 VIEWED / REJECTED
```

可以：

```text
Outcome = UNKNOWN
```

等待人工确认。

---

### User Applies Outside RoleOS

用户已经手动在其他渠道投递：

```text
可以创建 Manual Application Record
```

避免 RoleOS 再次重复投递。

---

### Rejection Without Reason

如果没有明确 Rejection Reason：

```text
不得由 Agent 编造原因
```

可以提出 Hypothesis，但必须标记为推测。

---

### Offer

`OFFER` 是 Application Outcome。

它不会自动：

```text
改变 Career Profile
停止所有其他 Workflow
```

由用户决定下一步。

---

# 25. Definition of Done

007 Application & Feedback 完成必须满足：

- [ ] V1 采用 Assisted Application；
- [ ] Application 明确绑定 Job / APPROVED Resume Version / Strategy Snapshot / Channel；
- [ ] Broad Apply 使用低成本投递策略；
- [ ] Targeted Apply 使用更完整 Review；
- [ ] 正式 Submit 前必须 Human Approval；
- [ ] CareerWorkflowStatus 与 ApplicationStatus 明确分层；
- [ ] 用户支持 APPROVE / EDIT / SKIP / DEFER；
- [ ] Submission 具备幂等保护；
- [ ] Workflow Retry 不会导致重复投递；
- [ ] 支持 SUBMISSION_UNKNOWN；
- [ ] 不确定 Side Effect 时不得自动 Retry；
- [ ] Browser Provider Fallback 尊重 Side-effect Boundary；
- [ ] Captcha / Human Verification 可暂停给用户；
- [ ] 支持 Automatic Outcome；
- [ ] 支持 Manual Outcome；
- [ ] Outcome 作为 append-only event，Application.currentStatus 由有效 Outcome / Submission 状态投影；
- [ ] Application 有 Timeline；
- [ ] Broad Funnel 至少包含 Applications / View / Reply / Interview / Offer；
- [ ] Targeted Feedback 关联 Requirement / Resume / Interview / Gap / Claim；
- [ ] Outcome 不直接覆盖 Career Fact；
- [ ] Feedback 使用 Observation → Hypothesis → Recommendation；
- [ ] Recommendation 不静默修改核心数据；
- [ ] Application Dashboard 可查看 Funnel 与单条 Application；
- [ ] Acceptance Scenarios 通过。

最终验收 Demo：

```text
Target Job
+
Approved Resume
        ↓
Application Prepare (PREPARED)
        ↓
CareerWorkflow.status = WAITING_APPROVAL
        ↓
User Approves
        ↓
Browser Submit
        ↓
SUBMITTED
        ↓
Outcome Tracking
        ↓
VIEWED / REPLIED / INTERVIEWING / REJECTED / OFFER
        ↓
Observation
        ↓
Hypothesis
        ↓
Recommendation
        ↓
Next Career Action
```

并必须验证异常链路：

```text
Submit
→ Browser Timeout
→ Application = SUBMISSION_UNKNOWN
→ CareerWorkflow.status = PAUSED_FOR_HUMAN
→ User Confirms Actual Result
→ Workflow Resume
```

如果 RoleOS 能够稳定回答：

```text
我投了什么？
用了哪份 Resume？
到底有没有提交成功？
后续发生了什么？
这些结果对下一步求职策略意味着什么？
```

则：

> **007 Application & Feedback 验收通过。**
