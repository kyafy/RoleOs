# Browser Provider Contract

## 边界

调用链固定为：

```text
JobIntelligenceService → JobSourcePort
BossJobSiteAdapter → BrowserRouter → BrowserProvider
```

`roleos-job` 只能看到 `RawJobSummary/RawJobDetail/JobSourceFailure`，不能看到 MCP、CSS/XPath、ElementRef、DOM、Tab 或 Session 类型。

## Provider 操作

每个 Provider 在自己的上下文内支持：

- `open(config) → ProviderContext`
- `navigate(context, url) → PageSnapshot`
- `snapshot(context) → PageSnapshot`
- `click(context, providerElementRef) → PageSnapshot`
- `fill(context, providerElementRef, value) → PageSnapshot`
- `close(context)`

`ProviderContext`、`providerElementRef` 和 `PageSnapshot` 必须携带同一 `providerInstanceId`。不一致时必须拒绝操作并返回 `CROSS_PROVIDER_REFERENCE`。

## Router 分类

| 分类 | 示例 | 行为 |
|---|---|---|
| RETRY_SAME_PROVIDER | 短暂超时、一次性页面未就绪 | 在有界次数内重试当前业务步骤 |
| SWITCH_PROVIDER | MCP 进程退出、工具不可用、可恢复导航失败 | 保存业务状态，关闭旧上下文，选择备用 Provider 后重新导航 |
| PAUSE_FOR_HUMAN | Captcha、人工验证、风险警告、敏感确认 | 持久化等待原因，只允许用户恢复 |
| TERMINAL_FAILURE | 不受信 URL、契约破坏、不可解析且无备用 | 保存失败并停止自动重试 |

## Provider 切换恢复

切换时只允许携带：

- searchId
- 业务步骤 step
- 受信 resumeUrl
- 页码或 externalJobId 等业务 cursor
- 已保存的 Job / Snapshot ID

禁止携带：

- ElementRef
- DOM/Accessibility Snapshot 句柄
- Tab/Page/Browser Handle
- Cookie/Storage State
- Provider Session ID

## 人工验证

快照检测到验证码、人工验证、平台风险警告或敏感确认时，Provider 返回结构化 `HumanVerificationRequired(reasonCode, displayMessage)`。Router 不调用任何规避工具；JobSearch 进入 `PAUSED_FOR_HUMAN`。恢复命令必须来自当前用户，并从 `resumeUrl + step` 重新抓取。

## 合同测试

所有 Provider（Fake、Playwright MCP、Kimi Bridge）共享以下合同测试：

1. 只接受受信 `https` URL。
2. Snapshot 与 ElementRef 不能跨 Provider 实例使用。
3. Captcha Fixture 返回人工暂停而非自动点击。
4. 日志不包含 Cookie、Token、Raw HTML/JD 或页面句柄。
5. `close` 幂等，失败上下文不可继续操作。
6. Router 切换后首次动作必须是 navigate/snapshot，而不是复用旧 ref。
