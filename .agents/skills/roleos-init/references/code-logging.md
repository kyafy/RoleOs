# RoleOS 代码日志规范

代码日志用于还原业务检查点、状态转换、外部交互和失败原因，不用于打印每行执行轨迹。统一使用
SLF4J 2 fluent API 或项目封装的等价结构化 API；禁止字符串拼接和 `System.out/err`。

## 日志格式

生产日志为单行 JSON，时间由 Logback 统一生成。固定字段：

| 字段 | 要求 |
| --- | --- |
| `timestamp` | UTC、ISO-8601、毫秒精度，例如 `2026-09-18T08:15:30.123Z` |
| `level` | `ERROR/WARN/INFO/DEBUG/TRACE` |
| `service` | 固定为服务名，例如 `roleos` |
| `version` | 发布版本或 commit，不使用开发者姓名 |
| `environment` | `local/test/staging/prod` |
| `event` | 稳定的点分小写事件名，例如 `workflow.transition.completed` |
| `message` | 简短中文说明，不能承载需要检索的动态字段 |
| `logger`、`thread` | 由框架生成 |
| `traceId` | 请求/任务链路标识；无链路时显式缺省，不伪造业务 ID |

按事件添加：`workflowId`、`jobId`、`upgradeId`、`commandId`、`errorCode`、`outcome`、
`durationMs`、`attempt`、`provider`。字段名使用 lowerCamelCase，持续时间统一为毫秒并以 `Ms` 结尾。
禁止把动态 ID、URL、异常消息或用户文本拼进 `event` 或 `message`。

开发环境可以使用可读文本布局，但字段语义必须与生产 JSON 一致。

## 必须记录的关键事件

- 应用启动完成/失败、安全配置摘要和优雅关闭；
- Workflow 创建、合法状态转换、等待人工确认、恢复、终止和重试耗尽；
- Approval 创建与决定，只记录 ID、决定和 Provenance，不记录完整职业内容；
- 外部 Agent、浏览器、Codex、数据库迁移等调用的完成、失败、超时和受控重试；
- 具有外部副作用的操作在确认前、执行结果和“不确定是否生效”状态；
- 认证/授权拒绝、路径越界、协议校验失败、限流和 Secret/敏感字段拦截；
- 关键幂等命中、并发冲突和恢复行为。

普通纯函数、getter/setter、循环迭代和无决策的 repository 成功调用不记录 INFO。禁止为每个方法
自动打印 entry/exit；需要性能诊断时使用指标、Trace 或临时受控 DEBUG。

## 日志级别

- `ERROR`：当前操作失败且需要告警/人工处理的非预期错误；记录一次完整异常堆栈。
- `WARN`：可恢复异常、降级、重试、冲突、安全拒绝或接近容量上限；不代表系统已失败。
- `INFO`：低频生命周期、业务检查点、状态转换和已确认外部动作结果。
- `DEBUG`：开发诊断所需的分支和脱敏摘要；生产默认关闭。
- `TRACE`：仅临时深度诊断，生产禁止常开。

同一异常只在负责处理或终止传播的边界记录一次。中间层若只是包装并继续抛出，不重复打印堆栈。
预期业务拒绝使用稳定 `errorCode`，不能全部记为 ERROR。

## Java 示例

```java
log.atInfo()
    .addKeyValue("event", "workflow.transition.completed")
    .addKeyValue("workflowId", workflowId.value())
    .addKeyValue("commandId", commandId.value())
    .addKeyValue("fromStage", fromStage)
    .addKeyValue("toStage", toStage)
    .addKeyValue("durationMs", duration.toMillis())
    .log("Workflow 状态转换完成");
```

失败日志：

```java
log.atError()
    .setCause(exception)
    .addKeyValue("event", "agent.call.failed")
    .addKeyValue("provider", providerName)
    .addKeyValue("errorCode", errorCode.code())
    .addKeyValue("attempt", attempt)
    .log("Agent 调用失败");
```

不得使用 `log.info("workflow {} moved from {} to {}", ...)` 承载所有结构化字段，也不得调用
`exception.printStackTrace()`。

## 敏感信息与基数控制

禁止记录 Authorization、Cookie、密码、Token、Secret、完整 JDBC URL、请求/响应 Body、完整简历、
完整 JD、Prompt、模型原始响应、浏览器 DOM/Snapshot、任意本地文件内容和用户自由文本。

- URL 只记录 scheme、受控 host 标识或路由模板，不记录 query/fragment；
- 用户、公司或项目标识仅在审计确有必要时记录内部 ID，不记录姓名；
- 指标标签不得使用 traceId/workflowId/jobId 等高基数字段；这些仅进入日志/Trace；
- 高频成功事件使用 DEBUG、指标或采样；ERROR、安全拒绝和副作用结果不得随机采样；
- 任何 sanitizer 失败时默认丢弃字段，而不是输出原值。

## 时间、作者与审计

- `timestamp`、时区和精度由 Logback encoder 统一配置，业务代码不得 `Instant.now()` 后拼进消息；
- 日志不包含代码作者、提交作者或个人邮箱；部署版本通过 `version/commit` 定位到 Git；
- 用户发起者如属于业务审计需求，应写入独立 Audit Event，并使用内部 actorId/actorType，不混用
  “代码作者”概念，也不在普通日志记录个人身份详情；
- 需要不可抵赖的业务审计时使用持久化 Audit Store，普通应用日志不能替代审计记录。

## 自动化验证

至少验证：

1. 固定字段、UTC 时间格式、event 命名和 `durationMs` 单位；
2. traceId/MDC 跨请求与受控异步边界传播并在结束后清理；
3. INFO 关键状态转换只记录一次，异常堆栈只在责任边界记录一次；
4. Secret、自由文本和完整 Payload 不出现在捕获日志中；
5. 生产默认关闭 DEBUG/TRACE，日志级别变更有受控配置入口；
6. 高频路径不会生成无界 INFO 日志，结构化字段不会形成指标高基数。
