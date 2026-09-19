# 执行编排

仅在运行环境不能依靠原生 Goal 连续推进，或用户明确要把 RoleOS SDD 嵌入自建调度器时读取本文件。

## 选择机制

按以下优先级选择，不要同时启用多个续跑器：

1. **Codex Goal**：首选。一个 Goal 覆盖 `tasks → implement 各 Phase → converge → 验收`，由运行时跨 turn 续跑。
2. **Codex SDK 或 app-server 事件**：自建宿主需要精确监听 turn 生命周期、处理审批或控制同一 thread 时使用。
3. **定时轮询**：仅在宿主拿不到事件流时降级使用。轮询 thread/turn 状态，不轮询或匹配自然语言回复。
4. **计划任务/heartbeat**：适合按时间再次检查长期外部状态；不适合作为 Phase 完成事件总线。

## 事件驱动循环

宿主对同一 Feature 最多允许一个 in-flight turn：

```text
start_or_resume_turn
  → consume lifecycle events
  → turn completed / needs input / failed
  → read tasks.md and verification evidence
  → COMPLETE: stop
  → WAITING_USER: surface the gate and stop
  → RETRYABLE_FAILURE: retry with a bounded policy
  → otherwise: send the next-phase continuation to the same thread
```

触发下一步的是宿主提供的 **turn terminal event**，不是 assistant 最后一段文字。收到 terminal event 后仍须读取工作区状态，按首个依赖已满足的 `[ ]` 任务决定下一 Phase。即使回复声称完成，只要仍有可执行的 `[ ]` 任务，就发送续跑指令。

建议的续跑指令保持稳定且幂等：

```text
继续使用 $roleos-sdd-autopilot 推进当前 Feature。重新读取 tasks.md 和验证证据，
从首个依赖已满足的未完成任务恢复；执行其所在 Phase，完成后继续下一 Phase。
仅在 Feature 验收完成、软门禁或我明确暂停时停止。
```

不要由调度器猜测具体 Task 是否完成，也不要把 Task ID 缓存在唯一外部状态中；`tasks.md` 才是恢复锚点。外部状态只需记录 `featureDir`、`threadId`、当前 turn、重试次数和是否已有 in-flight turn。

## 轮询降级

无法订阅事件时：

- 轮询结构化的 thread/turn 状态，直到进入 terminal、needs-input 或 failed；
- 使用递增间隔并设置总超时，避免固定高频请求；
- 每轮发送续跑前用 `threadId + terminalTurnId` 去重；
- 未确认上一 turn 已结束前不得发送下一条指令；
- 超时只表示“状态未知”，不得并发启动第二个 implement；
- 连续失败达到宿主重试上限后转人工处理，保留 `tasks.md` 现场。

## 被迫结束 turn 时的检查点

若运行时要求在 Feature 未完成时输出 terminal response，末尾附加机器可读检查点，供人和宿主诊断；它不是进度真相：

```text
AUTOPILOT_CHECKPOINT
status: CONTINUE | WAITING_USER | COMPLETE | FAILED
feature_dir: <absolute-or-repo-relative-path>
completed_phase: <phase-or-none>
next_phase: <phase-or-none>
next_task: <task-id-or-none>
reason: <concise reason>
```

宿主可以校验该检查点，但必须以 terminal event 和工作区事实决定是否续跑。`WAITING_USER` 只能用于本 Skill 定义的软门禁；`COMPLETE` 必须同时满足任务全勾选、`converge` 无遗漏和验收报告完成。
