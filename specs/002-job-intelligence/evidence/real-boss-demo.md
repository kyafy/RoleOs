# 真实 Boss Demo：历史记录与当前验收

日期：2026-09-20。关联任务：T068、T072。

- 先前页面读取返回 Boss 杭州首页，显示“登录/注册”和公开岗位链接；这是页面结构证据，不是 RoleOS 搜索成功证据。
- 现有 `BossSnapshotParser` 只识别 `JOB key=value` / `DETAIL key=value` 行。页面读取结果为 link/paragraph/generic 的 accessibility 树，生产解析能力尚不满足 FR-002；已追加 T072。
- 本次通过应用内浏览器打开一次 `query=Java` 搜索，创建请求超时；随后读取标签页为 `about:blank`。对该标签页再次导航后仍为空白。未取得真实搜索结果或详情。
- 用户确认已有 Boss/Kimi 环境。读取现有 Chrome 时，工具两次返回辅助功能与屏幕录制权限仍待授权；因此未能确认其会话或读取页面。
- 当前没有可报告的 RoleOS searchId、入库岗位数或完成时间。不得把浏览器首页访问、测试 Fixture 或 Fake Golden Scenario 计作 AC-001 / SC-001 通过。

恢复：在可连接的用户可见浏览器中确认现有会话，取得脱敏列表/详情结构，完成 T072 后，通过 RoleOS 发起一次低频搜索并核对 PostgreSQL 和 Job Pool。验证码由用户处理。

## 连接阻塞解除（2026-09-20）

用户提供 Kimi 官方安装命令后，已安装 WebBridge v2.0.15，确认服务运行于 127.0.0.1:10086，Chrome 扩展 2.0.13 已连接。使用独立会话 `roleos-002-validation` 打开 Java 搜索，页面跳转至 `/web/geek/jobs?query=Java`，成功读取登录后的岗位列表及页面内职位描述。通过公开 DOM 链接读取确认岗位具备 `/job_detail/{externalId}.html` 地址。

这是实际 Chrome/Kimi 连通性与页面结构证据；尚未通过 RoleOS 管道入库，T068 仍未完成。快照为结构化 accessibility JSON，薪资存在私有字体字符，不应推断数字。未保存账户菜单、消息、简历或其他个人内容为夹具。

## RoleOS 管道实测（2026-09-21）

- 通过本机 `local` Profile 的 RoleOS API 发起一次用户可见、低频的 Java/杭州搜索；请求身份和浏览器会话均未写入本文件。
- 脱敏搜索标识：`626a6932-6dca-4b31-8fc7-b632518826f7`；返回状态 `FAILED`，失败码 `BOSS_LIST_EMPTY_OR_CHANGED`，发现、标准化、拒绝及排序计数均为 `0`。
- 应用日志确认 Kimi Provider 已完成真实页面导航，耗时约 1.7 秒；随后因当前 Boss accessibility 卡片形态未能安全解析而终止。没有猜测字段、写入岗位或自动重试。
- 该记录完成“真实管道调用与证据留存”，但 AC-001 仍为 PARTIAL：需要修复当前列表解析，再取得至少一条 Canonical Job 入库证据。

## RoleOS 管道验收通过（2026-09-21）

- 先前失败根因经受控诊断确认为两项页面适配差异：岗位卡片异步渲染，且杭州在 Boss 搜索 URL 中使用已验证站点编码 `101210100`。适配器仅对已验证城市映射编码；未映射值不会猜测。
- Kimi 进程仅提取可见岗位卡片的公开标题、薪资、标签、位置与受信任详情链接；公司与 JD 仅从对应公开详情页补齐。未读取、保存或输出 Cookie、账号、浏览器存储或 Provider 句柄。
- 真实低频 RoleOS 搜索完成：脱敏搜索标识 `c5990048-eae0-4ea0-b7b2-4a6b1bdb372a`，状态 `COMPLETED`；发现 15、标准化 15、拒绝 14、排序 1。实际 Provider 为 `KIMI_WEBBRIDGE`，无失败码或人工暂停。
- 验证命令：`node --test scripts/test/kimi-webbridge-agent.test.mjs`（3/3）与 `mvn -B -ntp -pl roleos-browser -am test`（Browser 16/16）。该结果满足 FR-002、FR-003、FR-006 与 SC-001 的真实列表、详情和 Canonical Job 入库证据。
