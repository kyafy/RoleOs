# 数据模型：Job Intelligence

## 1. 设计不变量

1. `Job` 只保存外部岗位事实，绝不保存 Broad/Targeted/Skip。
2. 用户过滤、排序与策略全部归属 `JobCandidate(userId, jobId)`。
3. 缺失事实使用显式 Unknown/空值及状态，不用推断值填充。
4. 原始来源快照可追溯；普通日志不记录原始 JD、Cookie、Token 或 Provider 私有句柄。
5. Agent 只产生待校验的语义判断；最终分数、策略变化和搜索状态均由确定性代码产生。

## 2. 聚合与关系

```text
CareerProfile 1 ── * JobSearch
JobSearch     1 ── * SourceSnapshot
Job           1 ── * SourceSnapshot
CareerProfile 1 ── * JobCandidate * ── 1 Job
JobCandidate  1 ── 1 FilterEvaluation
JobCandidate  1 ── 0..1 RankingEvaluation
JobCandidate  1 ── * JobDecision
```

## 3. 实体

### 3.1 Job

| 字段 | 类型/约束 | 说明 |
|---|---|---|
| id | UUID，PK | Canonical Job ID |
| userId | UUID，NOT NULL | V1 用户隔离边界 |
| source | enum，固定支持 BOSS | 来源 |
| externalJobId | string，NOT NULL | 来源岗位 ID |
| title / normalizedTitle | string | 原值与去重值 |
| company / normalizedCompany | string | 原值与去重值 |
| city | string nullable | 缺失即 Unknown |
| salaryMin / salaryMax | integer nullable | 月薪，单位 CNY |
| salaryMonths | integer nullable | 未知不默认 12 |
| experienceMin / experienceMax | integer nullable | 年数 |
| educationRequirement | string nullable | 原始学历要求 |
| rawJd | text，NOT NULL | 来源 JD，API 默认不完整返回 |
| normalizedJd | text，NOT NULL | 仅做空白/格式规范化，不改写事实 |
| publishTime | instant nullable | 来源明确时才保存 |
| recruiterActivity | enum | JUST_NOW/TODAY/RECENT/STALE/UNKNOWN |
| sourceUrl | HTTPS URL | 只允许 Boss 受信 Host |
| sourceStatus | enum | ACTIVE/REMOVED/UNKNOWN |
| contentHash | SHA-256 | normalizedCompany/title/city/JD 组合摘要 |
| metadata | JSON object | 白名单扩展字段 |
| firstSeenAt / lastSeenAt | instant | 首次/最近抓取 |
| createdAt / updatedAt | instant | 审计字段 |

**唯一性**：`(user_id, source, external_job_id)` 唯一。内容摘要建立查询索引，但内容重复由事务内去重服务合并，避免跨来源错误硬约束。

### 3.2 SourceSnapshot

| 字段 | 类型/约束 | 说明 |
|---|---|---|
| id | UUID，PK | 快照 ID |
| jobId | UUID，FK | 可在规范化完成后关联 |
| searchId | UUID，FK | 来源搜索 |
| source/externalJobId/sourceUrl | string | 来源身份 |
| rawPayload | JSON/text | 原始结构化内容或可追溯快照 |
| contentHash | SHA-256 | 内容摘要 |
| provider | enum | PLAYWRIGHT_MCP/KIMI_WEBBRIDGE/FAKE |
| fetchedAt | instant | 采集时间 |
| parseStatus | enum | PENDING/SUCCEEDED/FAILED |
| failureCode | string nullable | 脱敏错误码 |

**唯一性**：`(job_id, content_hash)` 唯一；相同内容不重复存快照。

### 3.3 JobCandidate

| 字段 | 类型/约束 | 说明 |
|---|---|---|
| id | UUID，PK | 候选 ID |
| userId / jobId | UUID，唯一组合 | 一个用户对一个 Job 只有一个候选关系 |
| filterResult | PASS/REJECT/UNKNOWN | 汇总硬过滤结果 |
| rankingScore | decimal(5,2) nullable | 0..100；Reject 为 null |
| strategy | BROAD_APPLY/TARGETED_APPLY/SKIPPED | 用户当前策略 |
| strategyRecommendation | KEEP_BROAD/PROMOTE_TO_TARGETED/SKIP/REVIEW | 系统建议 |
| semanticAnalysisStatus | PENDING/SUCCEEDED/FAILED | 语义判断状态 |
| decisionReason | text nullable | 最近用户决策理由 |
| metadata | JSON object | 非核心展示信息 |
| version | integer | 乐观锁/冲突检测 |
| createdAt / updatedAt | instant | 审计字段 |

**默认**：新 Candidate 的 strategy 为 `BROAD_APPLY`。推荐不得改变 strategy。

### 3.4 FilterEvaluation

| 字段 | 类型/约束 | 说明 |
|---|---|---|
| id / candidateId | UUID | 一对一当前评估 |
| overallResult | PASS/REJECT/UNKNOWN | 汇总结果 |
| city/salary/experience/role | JSON RuleResult | 每项包含 result、reasonCode、explanation、factsUsed |
| evaluatedAt | instant | 评估时间 |
| ruleVersion | string | 可重放规则版本 |

**汇总**：任一 REJECT → REJECT；否则任一 UNKNOWN → UNKNOWN；否则 PASS。

### 3.5 RankingEvaluation

| 字段 | 类型/约束 | 说明 |
|---|---|---|
| id / candidateId | UUID | 当前 Ranking |
| ruleScore / semanticScore / totalScore | decimal 0..100 | 语义失败时 semanticScore 可空 |
| recommendation | enum | 与 Candidate 建议一致 |
| reasons / importantSignals / warnings | JSON string arrays | 不能为空列表（可为空数组） |
| ruleSignals / semanticSignals | JSON object | 白名单数值信号 |
| agentTraceId | string nullable | 只保存 Trace 引用，不保存敏感 Prompt |
| evaluatedAt / scoringVersion | instant/string | 重放和诊断 |

**权重约束**：Recruiter Activity 对总分贡献上限为 5 分；Core Fit（Role/Technical/Career）合计权重必须高于活跃度、发布时间等弱信号总和。

### 3.6 JobDecision

| 字段 | 类型/约束 | 说明 |
|---|---|---|
| id | UUID，PK | 决策记录 |
| candidateId / userId | UUID | 归属 |
| commandId | string，唯一 | 幂等键 |
| decision | PROMOTE/KEEP_BROAD/SKIP | 用户动作 |
| previousStrategy / resultingStrategy | enum | 可审计变化 |
| reason | string nullable | 用户理由 |
| decidedAt | instant | 决策时间 |

### 3.7 JobSearch

| 字段 | 类型/约束 | 说明 |
|---|---|---|
| id / userId | UUID | 搜索归属 |
| criteria | JSON | keywords、city、薪资/经验偏好、目标 Role |
| requestedProvider | AUTO/PLAYWRIGHT_MCP/KIMI_WEBBRIDGE | 用户选择 |
| activeProvider | provider nullable | 当前实际 Provider |
| state | CREATED/RUNNING/PAUSED_FOR_HUMAN/RETRYABLE_FAILED/FAILED/COMPLETED | 状态 |
| step | NAVIGATE_SEARCH/LIST_SNAPSHOT/DETAIL_FETCH/NORMALIZE/FILTER/RANK/PERSIST | 可恢复业务步骤 |
| resumeUrl | HTTPS URL nullable | Provider 切换后重新导航位置 |
| cursor | string nullable | 业务级页码/外部 Job ID，不是页面句柄 |
| waitReason / failureCode | string nullable | 脱敏原因 |
| attemptCount | integer | 有界重试计数 |
| startedAt / updatedAt / completedAt | instant | 时间线 |

## 4. 状态转换

### JobSearch

```text
CREATED → RUNNING
RUNNING → COMPLETED
RUNNING → RETRYABLE_FAILED → RUNNING
RUNNING → PAUSED_FOR_HUMAN → RUNNING
RUNNING → FAILED
```

- Provider 可恢复失败：保存当前业务 `step/resumeUrl/cursor`，销毁旧 Provider 上下文，选择备用 Provider，重新 Navigate/Snapshot。
- Captcha/Human Verification/Risk Warning/Sensitive Confirmation：只能进入 `PAUSED_FOR_HUMAN`，不得自动转回 RUNNING。
- 只有用户明确恢复命令可使 `PAUSED_FOR_HUMAN → RUNNING`。

### JobCandidate Strategy

```text
BROAD_APPLY --PROMOTE--> TARGETED_APPLY
BROAD_APPLY --KEEP--> BROAD_APPLY
BROAD_APPLY --SKIP--> SKIPPED
TARGETED_APPLY --KEEP--> TARGETED_APPLY
TARGETED_APPLY --SKIP--> SKIPPED
SKIPPED --重复命令--> SKIPPED
```

002 不提供自动从 SKIPPED 恢复的动作；需要未来明确业务需求后再增加。

## 5. 数据安全与日志

- 数据库可保存履行业务所需 Raw JD/快照；API 仅对当前用户返回，列表不返回 Raw Payload。
- Cookie、密码、Token、ElementRef、DOM handle、tab/session ID 不进入 Career DB。
- 日志只记录 ID、枚举状态、reasonCode、数量与耗时，不记录 rawPayload、rawJd、完整 criteria 或 Agent 原始文本。
