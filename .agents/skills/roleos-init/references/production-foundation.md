# RoleOS 生产基础设施基线

在创建或补齐配置、日志、运行脚本、Dockerfile 或 Compose 时使用本清单。这里的“生产基线”表示
具备安全默认值、可观测性、资源边界和可验证运行路径；最终生产参数仍由实际部署环境确定。

## 交付文件

根据已有工程保留等价命名；新工程至少提供：

```text
roleos-boot/src/main/resources/application.yml
roleos-boot/src/main/resources/application-local.yml
roleos-boot/src/main/resources/application-test.yml
roleos-boot/src/main/resources/application-prod.yml
roleos-boot/src/main/resources/logback-spring.xml
.env.example
.dockerignore
Dockerfile
compose.yaml
compose.prod.yaml             # 或职责等价的 production override
scripts/check-env.sh
scripts/build.sh
scripts/verify.sh
scripts/dev-up.sh
scripts/dev-down.sh
scripts/status.sh
scripts/logs.sh
```

不要仅为满足文件名而生成空壳。若仓库已有可靠的 `Makefile`、Taskfile 或脚本入口，可以复用，
但必须覆盖相同行为并在 Skill 输出中说明映射。

## 配置与启动摘要

- `application.yml`：共同配置、安全默认值、配置导入、Actuator 最小暴露和日志配置入口。
- `local`：开发便利项，只允许绑定 loopback 或明确的本地地址；Swagger/详细健康信息不能泄漏到
  `prod`。
- `test`：确定性配置，不读取开发者本机 Secret，不依赖共享数据库。
- `prod`：数据库凭据、可信代理、Cookie/TLS、外部 Runtime、允许目录和成本/容量限制必须显式
  提供；关键项缺失时启动失败。
- 重要配置使用一组有命名空间的 `@ConfigurationProperties` 与 Bean Validation；记录配置来源时
  只记录 profile/键是否存在，不输出原值。
- 启动摘要使用 allowlist。允许记录：版本、commit、active profiles、端口、脱敏数据库目标、
  Flyway、OpenAPI、管理端点、workspace 根、Adapter 模式、超时/并发/容量上限。
- denylist 至少覆盖名称包含 `password`、`secret`、`token`、`credential`、`cookie`、`authorization`、
  `prompt`、`resume`、`jdRaw` 的字段。未知字段默认不记录。

测试至少验证：local/prod profile 绑定成功、prod 缺失必填项启动失败、启动摘要不含测试 Secret、
所有配置键在 `@ConfigurationProperties` 或框架配置中有明确消费者。

## Logback 基线

`logback-spring.xml` 至少具备：

- `local` profile：可读控制台格式，包含 timestamp、level、thread、logger、traceId；
- `prod` profile：JSON stdout，固定字段包含 timestamp、level、service、version、environment、
  logger、thread、message、traceId；可用时包含 workflowId/jobId/upgradeId；
- 异步 appender：队列容量、discarding threshold、neverBlock/阻塞策略显式配置；ERROR 不得因普通
  背压策略静默丢失；
- 可选文件 appender：按日期和大小滚动，配置单文件上限、保留天数与 totalSizeCap，启动时创建
  或验证目录；容器环境仍以 stdout 为主；
- 日志级别通过受控环境变量调整，生产默认 INFO，数据库 SQL、认证头和第三方 SDK wire log 默认关闭；
- MDC 生命周期：请求入口创建/校验 traceId，响应回写，跨受控异步边界传播，finally 清理；
- 脱敏：结构化参数使用 allowlist/专用 sanitizer，不依赖开发者记住不打印敏感字段。

测试不能只做 XML 存在性断言。至少加载 Logback 配置并捕获一条日志，校验 profile、JSON 字段、
traceId 和测试 Secret 不出现；滚动策略可通过小阈值测试配置验证。

## 启动与运维脚本

所有脚本使用明确 shebang 与 `set -Eeuo pipefail`，从脚本位置解析仓库根目录，并给出稳定退出码。

- `check-env.sh`：检查 Java 21、Maven、Docker/Compose 和必要端口；只报告 Secret 是否设置。
- `build.sh`：可重复构建，不跳过默认测试；允许显式参数选择快速模式。
- `verify.sh`：运行项目质量门禁，保留原始失败码。
- `dev-up.sh`：先启动依赖并等待健康，再启动应用；避免重复实例，记录 PID/Compose project。
- `dev-down.sh`：只停止本项目拥有的 PID/Compose project，重复执行成功，不删除持久卷。
- `status.sh`：同时报告进程、容器和健康端点，状态不一致时返回非零。
- `logs.sh`：按服务查看/跟随日志，默认不导出敏感环境变量。

不得用宽泛 `pkill java`、端口占用即杀进程、`rm -rf` 工作区或默认 `docker compose down -v`。

## Dockerfile 与 Compose

Dockerfile 使用多阶段构建或可靠的预构建制品输入，运行阶段满足：

- 固定兼容的 JRE 21 基础镜像版本策略；
- 非 root 用户、最小文件权限、明确工作目录；
- JVM 容器参数、UTF-8、时区策略和优雅关闭；
- OCI 标签或等价的版本/commit 元数据；
- 健康检查与 Spring Boot 探针对齐；
- 不复制 Maven 缓存、`.git`、Secret、测试报告或本地工作目录。

基础 Compose 负责可重复本地开发；production override 负责更严格边界。至少包含：

- RoleOS 与 PostgreSQL 服务、专用 network、命名 volume；
- PostgreSQL healthcheck，RoleOS 使用健康依赖并有自身 healthcheck；
- `restart` 策略、graceful stop、日志轮转上限；
- 明确端口绑定；数据库默认不在 production 对公网发布；
- 资源限制或可配置的 CPU/内存边界；
- 应用容器尽量 `read_only`、`cap_drop: [ALL]`，只给 `/tmp` 和明确工作目录写权限；
- `.env.example` 只含无效示例；production 不允许默认密码或把 Secret 写入 Compose 文件；
- Playwright/OryxOS 等尚未验证的服务通过 profile/override 隔离。

验收至少执行 `docker compose config`、镜像构建、健康启动、重启恢复、持久化验证、日志大小边界
检查和无卷删除的停止流程。若环境没有 Docker，必须把这些项目标为未验证，不能宣称生产可用。

## 通用代码放置决策

不要以“全局通用”为理由创建依赖无方向的 `core/base/common`：

| 能力 | 默认归属 |
| --- | --- |
| 领域错误、领域值对象、框架无关 Port | `roleos-domain` |
| Use Case 命令/结果、应用级失败 | `roleos-application` |
| API 响应、HTTP 错误映射、Controller Advice | `roleos-web` |
| Spring 装配、启动检查、安全配置摘要 | `roleos-boot` |
| JPA/Flyway/PostgreSQL 配置 | `roleos-storage` |
| 浏览器、Codex、OryxOS 专属配置 | 对应 Adapter 模块 |

确需共享模块时，先证明有至少三个真实消费者、API 稳定、无 Spring/JPA/Web 依赖、不会形成循环依赖，
并通过 Plan/架构门禁。共享模块应按能力命名；`core` 或 `base` 不能替代职责定义。
