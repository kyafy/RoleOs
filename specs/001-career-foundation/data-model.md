# Phase 1 数据模型：Career Foundation

所有主实体包含 `id`、`userId`、创建/更新时间、乐观版本；写命令包含 `commandId`。

| 实体 | 关键字段与约束 |
| --- | --- |
| CareerProfile | 基本资料、方向、偏好；每 userId 一份。 |
| Experience | type（工作、项目、个人、开源、学习）、真实上下文、完整性标记；可不完整但不可虚构。 |
| Project | 名称、历史来源、项目性质、仓库引用；关联 Experience/Skill/Capability。 |
| Skill | displayName、normalizedName、自评与验证层级；同 userId 下名称基础唯一。 |
| SkillSource / FactProvenance | 来源、确认状态、支撑资产或证据引用、时间；允许多条。 |
| Capability | 名称；与 Skill/Experience/Project 多对多，无自动评分。 |
| ResumeImport / FactCandidate | 批次、受控源引用、候选类型、负载、定位、审阅状态；初始 WAITING_CONFIRMATION。 |
| CandidateDecision | CONFIRM/EDIT/REJECT、用户、时间、commandId；拒绝不改事实。 |
| WorkflowInstance / WorkflowEvent | 类型、阶段、状态、等待原因、上下文、重试、版本；事件只追加。 |
| Approval / IdempotencyRecord | 审批决定、版本、备注；命令结果去重。 |

```text
CREATE → RUNNING
RUNNING → WAITING_USER_INPUT | WAITING_APPROVAL | PAUSED_FOR_HUMAN | RETRYABLE_FAILED | COMPLETED | CANCELLED
WAITING_* → RUNNING | CANCELLED（DEFERRED 保持 WAITING_APPROVAL）
RETRYABLE_FAILED → RUNNING | FAILED | CANCELLED
FAILED | COMPLETED | CANCELLED → 不可推进
```

审批的状态改变必须校验 userId、当前状态和版本；不兼容或重复决定返回当前状态。
