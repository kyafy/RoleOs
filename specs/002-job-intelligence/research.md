# Phase 0 Research：Job Intelligence

## 决策 1：Playwright MCP 的 Java 接入

**Decision**：在 `roleos-browser` 使用 MCP Java SDK 2.0.0 BOM 的 `mcp-core` 与 `mcp-json-jackson2`，默认以 STDIO 启动 `npx -y @playwright/mcp@latest`；同时允许配置 Streamable HTTP `/mcp` 端点。运行时先进行协议/能力协商和工具发现，再调用浏览工具，不把工具实现类泄漏到 Job 模块。

**Rationale**：官方 SDK 提供 STDIO、SSE 与 Streamable HTTP、协议协商和工具调用；Jackson 2 绑定与 Spring Boot 3.5 当前序列化栈兼容。Playwright 官方文档明确支持标准 MCP 客户端、Node.js 20+、可见浏览器和独立 HTTP 服务。

**Alternatives considered**：手写 JSON-RPC/MCP（协议和生命周期风险高）；直接引入 Playwright Java（违反“Playwright MCP 为 Primary”且扩大 Browser Runtime 自研）；仅用 HTTP 抓页面（无法复用用户登录会话及人工验证流程）。

**Sources**：https://java.sdk.modelcontextprotocol.io/latest/quickstart/、https://java.sdk.modelcontextprotocol.io/latest/client/、https://github.com/microsoft/playwright/blob/main/docs/src/getting-started-mcp.md

## 决策 2：Kimi Browser Extension 作为 Fallback

**Decision**：定义 Provider 中立的 BrowserProvider 合同，并为 Kimi 提供“可配置本地 Agent 命令 + 严格 JSON 输入输出”的适配层。适配层不访问扩展内部 CDP、Cookie 或标签页句柄；真实环境由用户安装 Kimi Browser Extension 及其本地 Bridge/Agent，RoleOS 只交付任务与接收结构化快照。契约测试使用 Fake Kimi Transport，真实验收使用用户本机已配置的 Local Agent。

**Rationale**：Kimi 官方说明扩展通过本地 Bridge Service 接收本地 Agent 指令、在用户浏览器内执行，并支持 Codex 等本地 Agent，但没有公开稳定的 Java wire protocol。用外部命令边界可满足主备与手动 Provider，同时避免绑定未公开实现。

**Alternatives considered**：反向工程本地 Bridge 协议（不稳定且存在安全风险）；直接使用 CDP（违反 BrowserProvider 隔离与“不自研 Browser Runtime”）；将 Kimi 排除（违反 Feature 明确主备要求）。

**Sources**：https://www.kimi.com/en/help/kimi-webbridge/kimi-webbridge-introduction、https://www.kimi.com/en/help/kimi-webbridge/kimi-webbridge-how-it-works

## 决策 3：Job 模型与模块依赖方向

**Decision**：Job Intelligence 的聚合、端口和用例放在 `roleos-job`；`roleos-browser`、`roleos-storage`、`roleos-web` 分别依赖并实现其外部适配。`roleos-job` 不依赖浏览、存储或 Web 模块，也不包含选择器、MCP 类型或 Provider 句柄。

**Rationale**：保持 `Career Domain → JobSiteAdapter → BrowserRouter → BrowserProvider`，同时避免 `roleos-job` 与 `roleos-browser` 循环依赖。JobSourcePort 使用来源中立的 RawJobSnapshot，BossJobSiteAdapter 在浏览模块实现。

**Alternatives considered**：把所有 Job 类型放进 `roleos-domain`（扩大公共核心）；让 Job 模块依赖 Browser（形成基础设施反向依赖）；在 Web Controller 直接驱动浏览（绕过用例和恢复状态）。

## 决策 4：数据持久化与历史范围

**Decision**：新增 Job、Job Snapshot、Job Candidate、Filter Evaluation、Ranking Evaluation、Job Decision、Job Search 表。Canonical Job 保存当前标准化事实；Source Snapshot 保存每次内容摘要与原始 JSON，但相同摘要不重复插入。V1 不建设完整事件溯源。

**Rationale**：同时满足原始数据可追溯、JD 变化可识别、用户策略不污染 Job 事实和恢复搜索；避免为了 V1 引入事件存储复杂度。

**Alternatives considered**：只保存当前 Raw JSON（无法证明变化）；每次抓取创建新 Job（破坏去重与用户决策）；完整 Event Sourcing（超出 V1）。

## 决策 5：确定性过滤与 Ranking

**Decision**：每条过滤规则输出 Pass/Reject/Unknown + code + explanation；汇总规则为“任一 Reject → Reject，否则任一 Unknown → Unknown，否则 Pass”。Reject 不排名，Pass/Unknown 进入轻量 Ranking。总分由归一化 Rule Signals 与已校验 Semantic Signals 确定性加权；Recruiter Activity 的最大贡献受硬上限约束，不能逆转明显的核心相关性差距。

**Rationale**：直接落实 Missing Data ≠ Matched、Agent for Judgment/Code for Rules 与弱活跃度信号原则；单项输出便于用户理解和测试。

**Alternatives considered**：Agent 直接决定过滤/总分（不可重复）；Unknown 自动 Pass（掩盖风险）；只存总分（不可解释）。

## 决策 6：Agent 失败策略

**Decision**：SemanticRankingPort 返回严格结构化对象，入库前校验 schema、分值范围、非空解释及信号白名单。失败或非法输出时保存 `FAILED`/`PENDING` 状态及脱敏错误，不删除 Job，也不伪造语义分；Ranking 可显示规则基线分与“语义分析未完成”。

**Rationale**：结构化校验、可恢复失败和真实性红线要求 Agent 结果不能未经校验成为事实。

**Alternatives considered**：失败即丢弃岗位；用默认语义高分补齐；保存原始自由文本并由 UI 猜测。

## 决策 7：Job Pool UI

**Decision**：使用 Spring MVC + Thymeleaf 提供服务端渲染 Job Pool，同时保留 OpenAPI REST 契约。页面通过 Progressive Enhancement 执行筛选、刷新与决策，不新增 Node 前端工程。

**Rationale**：现有工程已具备 Spring MVC/Security，服务端渲染能以最小新增依赖实现生产可用的认证、错误处理、可访问链接与空/失败状态，并减少 V1 构建复杂度。

**Alternatives considered**：新增 React/Vue SPA（额外构建链与大量非核心任务）；只提供 Swagger（不满足 Job Pool Web UI）；静态 HTML 直接请求 API（认证/错误体验较弱）。

## 决策 8：可观察性与敏感信息

**Decision**：搜索、抓取、解析、过滤、排序、决策和 Provider 切换记录统一事件名、traceId、userId 哈希、searchId/jobId、provider、duration/status/reasonCode；禁止记录 Raw JD 全文、Cookie、Token、页面句柄或完整个人偏好。提供搜索计数、失败计数、人工暂停、Provider 切换和阶段耗时指标。

**Rationale**：满足生产日志和故障恢复所需信息，同时遵守最小暴露职业信息与浏览凭据原则。

**Alternatives considered**：记录完整页面便于调试（敏感数据风险）；只写自由文本（不可聚合）；完全不记录失败上下文（不可运维）。
