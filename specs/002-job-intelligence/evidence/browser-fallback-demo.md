# 真实 Provider Fallback：历史记录与当前验收

日期：2026-09-20。关联任务：T069、T075。

- Fake Browser 合同及此前 PostgreSQL Golden Scenario 已覆盖 Playwright 失败后 Kimi 重新导航、旧引用不可复用；这些不是实际 Kimi Extension 连接证据。
- 用户确认已有 Boss/Kimi 环境，但尚未明确所在浏览器及本地 Agent 命令/配置位置。
- 当前执行环境未设置 `ROLEOS_KIMI_WEBBRIDGE_COMMAND`，项目 `.env` 未包含 Kimi/Playwright Provider 配置。仓库默认 Kimi 关闭、命令为空。此检查只说明当前进程配置缺失，不代表用户未安装扩展。
- 当前工具读取 Chrome 仍等待操作系统授权，尚未连接实际扩展。
- 代码复核发现 Boss Adapter 保存共享 `requestedProvider` / `summaries`，且构造随机 searchId；Orchestrator 恢复时未还原技能/职业目标或精确详情恢复位置。T075 跟踪这些真实恢复缺口。

恢复：确认用户现有 Kimi 本地命令并在本地配置（不提交凭据），完成 T075，再通过 RoleOS 制造一次可恢复的 Playwright 故障，验证真实 Kimi 重新导航、使用相同业务 searchId，并保留脱敏证据。

## Kimi 连接验证（2026-09-20）

已执行用户指定的官方安装器并读取随安装提供的 `kimi-webbridge` Skill 与 operations 文档。服务 v2.0.15 与 Chrome 扩展 2.0.13 已连接；官方 `navigate`、`snapshot`、只读 DOM 链接查询均成功。此前系统 UI 访问权限不再是本任务的连接前提。

已确认正式公开协议是本地 HTTP `/command` 的 `{action,args,session}` 请求和 `{ok,data}` 响应；现有 RoleOS Provider 使用本地进程 `{operation,arguments}` / `accessibilityText` 契约，仍需实现协议转换脚本。`kimi-webbridge` 主程序只管理服务，不能直接作为这一进程契约的命令。扩展连通不等于 RoleOS Adapter 或真实主备切换已通过。

## RoleOS 真实 Fallback（2026-09-21）

- 以受控的本地 Playwright MCP 初始化失败配置发起一次真实 RoleOS 搜索，避免向 Boss 发送额外操作；请求 Provider 为 `PLAYWRIGHT_MCP`。
- 脱敏搜索标识：`e2ee6cfd-360c-4986-8f19-b9272eb562ca`。日志依次记录 `MCP_INITIALIZATION_FAILED`、`browser.provider.switched`（Playwright → Kimi）以及 Kimi 的 `browser.navigate.completed`，证明备用 Provider 从持久化业务 URL 重新导航。
- 实测暴露并修复了 Router 原先未将 Provider `open` 置于可切换边界的问题；新增初始化失败切换测试后，`mvn -B -ntp -pl roleos-browser -am test` 通过（Browser 13/13）。
- 后续仍因当前 Boss 列表无法安全解析而返回 `BOSS_LIST_EMPTY_OR_CHANGED`；这不否定 Fallback 验收，但 AC-001 的真实入库仍保持 PARTIAL。
