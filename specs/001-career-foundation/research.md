# Phase 0 研究结论：Career Foundation

## 职业事实与来源

**决策**：职业资产采用独立 `FactProvenance` 保存来源；CareerProfile、Experience、Project、Skill、Capability 均保留 `userId`。同一资产可有多条来源，Agent 推断与生成文案均为非事实记录。

**理由**：支持审计、多来源和未来证据追加，满足“Agent Inference 不等于 User Fact”。

**备选**：单一 source 字段无法表达多来源；直接写推断违反真实性，均不采用。

## 技能与导入确认

**决策**：Skill 以 `normalizedName` 基础去重，SkillSource 独立表达自我声明、经历识别、证据验证；自评与验证层级分开。简历使用 `ResumeImport` 批次和逐项 `FactCandidate`，仅确认/编辑后写入事实，拒绝保留审计。

**理由**：确保未确认写入率为零，且不会把自评层级伪装为验证层级。

**备选**：每个来源建立独立技能、整份简历全局确认或导入即写入，均无法满足审阅与真实性边界。

## 工作流、审批与恢复

**决策**：`WorkflowInstance` 是长期任务唯一事实源，`WorkflowEvent` 只追加审计；显式状态机和审批原子转移，命令用幂等键、审批用版本条件更新。重启恢复视图和待办，不自动重放运行中的动作。

**理由**：保证审批不可跳过、重复提交不重复推进且重启可恢复。

**备选**：Agent 会话、内存状态或引入 Temporal/Redis 工作流均不满足恢复或 V1 最小复杂度。

## 接口、安全与测试

**决策**：`/api/v1` 提供职业档案、候选审阅、工作流和审批资源；认证主体决定 userId。Fake Resume Adapter 的输出以契约测试约束。测试覆盖领域规则、持久化恢复、Web 安全与 Golden Scenario。

**理由**：复用现有默认拒绝安全、统一错误响应和 Trace 基线，避免真实解析服务带来的敏感数据风险。
