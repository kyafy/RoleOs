# Quickstart：Job Intelligence 验证指南

## 1. 前置条件

- Java 21、Maven 3.9+、Docker/Compose。
- PostgreSQL 由项目 Compose 启动。
- 真实 Playwright MCP 验收需 Node.js 20+，并允许启动用户可见浏览器。
- 真实 Boss 验收由用户在可见浏览器内自行登录；不要把账号、Cookie 或 Token 写入配置、命令行、日志或 Fixture。
- Kimi Fallback 真实验收需用户已安装 Kimi Browser Extension，并按其官方方式连接本地 Agent；无该环境时只能完成 Fake Transport 合同验证，不能宣称真实 Kimi 验收通过。

## 2. 启动基础设施与服务

```bash
docker compose up -d postgres
mvn -B -ntp -pl roleos-boot -am spring-boot:run -DskipTests
```

确认：

```bash
curl -s http://localhost:8080/actuator/health
```

期望 `status=UP`，Flyway 已执行到 `V7__job_intelligence.sql`，启动摘要只显示 Provider 是否启用，不显示命令参数、Cookie 或 Token。

## 3. 自动化验证

### 3.1 领域与确定性管道

```bash
mvn -B -ntp -pl roleos-job -am test
```

必须覆盖：

- Canonical Job 与 JobCandidate 策略分离；
- Source ID 与 Content Hash 双层去重；
- 20 条 Fixture 的 City/Salary/Experience/TargetRole Pass、Reject、Unknown；
- Unknown 不等于 Pass，Reject 不进入 Ranking；
- Recruiter Activity 只产生弱权重；
- 非法 Agent 输出变为 Failed，Job 和规则信号仍保留；
- 推荐不会自动 Promote，决策幂等。

### 3.2 Browser 与 Boss Adapter 合同

```bash
mvn -B -ntp -pl roleos-browser -am test
```

必须覆盖 [browser-provider-contract.md](contracts/browser-provider-contract.md)：跨 Provider Ref 被拒绝、Playwright 可恢复失败后 Kimi 从业务 URL/步骤重新导航、Captcha 进入人工暂停、Boss Fixture 能解析列表和详情、日志无敏感数据。

### 3.3 PostgreSQL、API 与 UI

```bash
mvn -B -ntp -pl roleos-storage,roleos-web -am test
```

必须覆盖：V7 迁移、唯一约束、快照变更、用户隔离、分页筛选、详情、搜索恢复、Promote/Keep/Skip 幂等、统一错误码和 Job Pool 页面空/失败/Unknown 状态。

页面搜索交互还需运行原生 JavaScript 合同测试（无额外 npm 依赖）：

```bash
node --test roleos-web/src/test/js/job-search.test.mjs
```

覆盖人工暂停后的显式恢复提示、失败/终态动作、CSRF 和幂等键传递，以及网络失败不自动重放命令。

### 3.4 Golden Scenario

```bash
mvn -B -ntp -pl roleos-boot -am test \
  -Dtest=JobIntelligenceGoldenScenarioIT \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Golden Scenario 使用 Fake Provider 和 PostgreSQL，证明：Discovery → Normalize → Dedup → Filter → Ranking → Job Pool → 推荐 → 用户 Promote；并额外证明 Playwright Failure → Kimi → 从 Business State 恢复。

## 4. REST/UI 验证

以当前项目认证方式取得会话或 Token 后，按 [job-intelligence-api.yaml](contracts/job-intelligence-api.yaml) 验证：

1. `POST /api/job-searches` 发起搜索，携带唯一 `Idempotency-Key`。
2. `GET /api/job-searches/{searchId}` 查看步骤、Provider 和计数。
3. `GET /api/jobs?filterResult=PASS` 查看候选列表。
4. `GET /api/jobs/{jobId}` 查看 Raw/Normalized JD、逐项 Filter 与 Ranking Explanation。
5. `POST /api/jobs/{jobId}/decisions` 执行 PROMOTE；重复相同幂等键结果不变。
6. 浏览 `/jobs`，验证 Broad/Targeted/Skipped/Pass/Unknown 筛选、详情、来源链接和策略操作。

## 5. 真实 Boss / Playwright MCP 验收

1. 启用 Playwright MCP Provider，保持 headed 模式。
2. 用户在可见浏览器内完成 Boss 登录，确认没有验证码待处理。
3. 通过 UI 发起一次小范围、低频搜索。
4. 验证至少一个真实岗位拥有 BOSS 来源、外部 ID、来源 URL、Raw Snapshot、Canonical Job、Filter 和 Ranking。
5. 若出现 Captcha/风险提示，验证搜索进入 `PAUSED_FOR_HUMAN`；系统没有自动点击或绕过。用户处理后调用 Resume 并从保存的业务步骤继续。
6. 在测试配置中制造 Playwright 可恢复失败，验证 Router 关闭旧上下文、选择 Kimi，并从 `resumeUrl/step` 重新导航；若本机未配置 Kimi，则记录为外部验收未满足，不得以 Fake 结果冒充。

## 6. 全量质量门禁

```bash
mvn -B -ntp -Pquality verify
```

若依赖漏洞数据源暂不可用，可单独记录网络/NVD 证据，但最终 Feature 验收前必须补跑成功；不得把 `-Ddependency-check.skip=true` 结果描述为完整质量门禁。

## 7. 完成证据

验收报告至少记录：

- 已完成的 Tasks/Phase；
- 各模块测试、Golden Scenario、全量质量门禁结果；
- 真实 Boss 搜索时间、searchId、岗位数量与脱敏截图/日志引用；
- Playwright→Kimi 恢复或外部环境阻塞证据；
- Captcha 人工暂停验证；
- Spec/Plan/Tasks/Constitution 漂移检查；
- 未完成项、阻塞原因与下一步。
