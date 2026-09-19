---
name: roleos-init
description: >-
  初始化或补齐 RoleOS 的工程地基：Java 21 + Spring Boot 3.x Maven 多模块骨架、
  PostgreSQL/Flyway、分环境配置与安全启动摘要、生产日志基线、启动运维脚本、
  Docker/Compose、本地运行与可观测性、统一 API/异常契约、代码质量与安全门禁、CI，
  并建立 RoleOS 的 Port/Adapter 与 OryxOS Runtime 隔离边界。当用户要初始化 RoleOS、
  创建工程骨架、补齐基础设施、配置开发规范、日志监控、容器运行、安全扫描或 CI 时使用；
  不要将其用于实现岗位分析、经历匹配、工作流业务规则或其他产品功能。
---

# RoleOS 初始化 Skill

为 RoleOS 建立可验证、可演进的工程地基。这个 Skill 只处理跨模块基础设施和工程约束，
不实现任何 Career Domain 产品能力。这样可以让后续 SDD 从清晰的边界和可靠的质量门禁开始。

运行前提：需要 JDK 21、Maven、Git 和一个已存在或可创建的 RoleOS 仓库；完整初始化还需要
Docker/Compose。缺少 Docker 时可以生成相关文件，但必须将容器验证标为未执行。

## 适用范围

在以下场景使用：

- 新建或空白的 RoleOS 仓库，需要建立 Maven 多模块工程；
- 已有 RoleOS 仓库缺少持久化、可观测性、API 规范、质量检查或 CI；
- 需要在不实现业务功能的前提下，修复或补齐工程地基。

不要用本 Skill：

- 实现 Job Intelligence、JD & Experience Match、Experience Builder、Project Upgrade、
  Claim-Evidence、Resume 或 Application 的业务逻辑；这些必须通过对应的 Spec Kit
  User Story 实现。
- 改变项目宪法、领域边界、产品范围或 OryxOS 集成策略；先走架构决策与 Spec Kit 流程。
- 将 OryxOS 源码合并到 RoleOS，或让 Domain 直接依赖 OryxOS 内部类。

## 事实来源与不变量

实施前必须阅读：

1. `.specify/memory/constitution.md`；
2. `AGENTS.md`；
3. `docs/RoleOS技术方案.md` 的工程结构、OryxOS 集成、部署和测试章节；
4. `docs/RoleOS_AI编程指南.md` 的 Foundation、质量门禁和任务拆分章节。

必须遵守：

- RoleOS 是独立项目；OryxOS 是可替换的 Agent Runtime，通过
  `roleos-runtime-oryx` 接入。
- Career Domain 只依赖 Port。只有 `roleos-runtime-oryx` 能直接依赖 OryxOS API。
- Career Workflow、Career Fact 和 Claim/Evidence 是 RoleOS 自己的持久化业务资产，
  不能保存在 Agent Session 或 OryxOS Memory 中。
- 不写入明文密钥、Token 或个人职业数据；配置使用环境变量占位。
- 不因基础设施初始化而绕过 Human Approval、Evidence Policy 或其他宪法原则。

## 执行前检查

先检查并向用户报告：

- 工作目录、Git 状态和是否已有业务代码；
- JDK 版本、Maven 可用性、Docker/Compose 可用性；
- 是否已存在 `pom.xml`、模块目录、CI、质量插件或数据库迁移；
- 是否已存在 `application*.yml`、`logback-spring.xml`、Dockerfile、Compose 文件和启动脚本，
  以及它们是否只覆盖开发环境；
- `.specify/memory/constitution.md`、`AGENTS.md` 与技术方案是否存在；
- 现有配置是否与本文档的不变量冲突。

默认值如下；若用户提供其他值或已有工程配置，应优先保留用户选择：

| 参数 | 默认值 |
| --- | --- |
| 根 artifactId | `roleos` |
| Maven groupId | `io.roleos` |
| 根包名 | `io.roleos` |
| 本地 HTTP 端口 | `8080` |
| Java | `21` |
| 数据库 | PostgreSQL |
| OryxOS 初始模式 | `AgentRuntimePort` + `FakeAgentRuntime`；真实 Adapter 仅在依赖坐标和兼容性验证后接入 |

若仓库非空且初始化会覆盖 `pom.xml`、CI、质量配置或运行配置，必须先列出差异并获得用户确认。
不自动提交 Git；只有用户明确要求时才按阶段提交。

## 初始化流程

### 1. 创建 Maven 模块骨架

创建父 `pom.xml`（`packaging=pom`），统一管理 Java 21、依赖版本、插件版本和构建规则。
按技术方案创建以下 15 个模块；只创建最小可编译骨架，不实现业务功能：

```text
roleos-domain
roleos-application
roleos-workflow
roleos-job
roleos-experience
roleos-upgrade
roleos-evidence
roleos-resume
roleos-application-tracking
roleos-browser
roleos-codex
roleos-runtime-oryx
roleos-storage
roleos-web
roleos-boot
```

建立最小依赖方向：

```text
roleos-domain
       ↑
roleos-application / roleos-workflow
       ↑
adapter modules（storage、web、browser、codex、runtime-oryx）
       ↑
roleos-boot
```

模块骨架中不得为了“先跑起来”而让 Domain 引用 Web、Storage、Browser、Codex 或 OryxOS。

不要默认新增名为 `core`、`base` 或 `common` 的万能模块。初始化阶段按依赖方向放置通用能力：

- 领域错误类型和值对象属于 `roleos-domain`，必须与 Spring、HTTP 和数据库无关；
- Use Case 级失败与应用契约属于 `roleos-application`；
- HTTP 错误响应、状态码映射、`@RestControllerAdvice`、Web 安全和序列化配置属于
  `roleos-web`；
- Spring Boot 装配、启动期校验和安全配置摘要属于 `roleos-boot`；
- 数据库、浏览器、Codex、OryxOS 等配置分别留在其 Adapter 模块。

只有至少三个模块确实需要同一组**无框架、稳定、低层级**原语，且技术方案/Plan 已批准新增
模块时，才可建立窄职责的共享模块；不得把 Controller DTO、JPA Entity、Spring Bean、工具类、
业务枚举和异常映射混放到共享模块。新增 Maven 模块属于架构变更，不能由本 Skill 静默决定。

### 2. 建立运行、持久化与本地开发基础

- 在 `roleos-boot` 提供最小 Spring Boot 启动入口与健康检查。
- 生成分环境配置：基础 `application.yml` 只放共同且安全的默认值；开发、测试、生产差异进入
  `application-local.yml`、`application-test.yml`、`application-prod.yml`。使用经过 Bean
  Validation 的 `@ConfigurationProperties`，关键生产配置缺失时 fail fast，禁止散落的
  `@Value` 和静默不安全默认值。
- 为重要配置生成启动期安全摘要日志：记录 active profiles、服务端口、数据库主机/库名的脱敏值、
  Flyway 开关、OpenAPI/Actuator 暴露、工作目录、Runtime/Adapter 模式和关键限额；绝不记录
  密码、Token、完整 JDBC URL 查询参数、Cookie、个人数据或模型 Prompt。配置文件本身要用注释
  说明环境变量、单位、默认行为和生产要求，但不得用日志输出代替配置校验。
- 在 `roleos-storage` 配置 PostgreSQL 连接、Flyway 基线和 Testcontainers 支持；不要创建
  Career Domain 业务表，业务迁移由对应 User Story 添加。
- 提供示例环境变量文件（不含真实凭据）、`.gitignore`、多阶段 `Dockerfile`、`.dockerignore`
  和 Compose。Compose 至少初始化 RoleOS 与 PostgreSQL；未就绪的外部 Runtime 使用 profile
  或 override 文件声明为可选服务，不得伪造可运行集成。
- 同时生成可执行且幂等的开发脚本，至少覆盖环境检查、构建/验证、启动、停止、状态和日志查看。
  脚本必须使用严格模式、显式工作目录、可恢复 PID/Compose 状态和清晰退出码；不得隐藏失败、
  写入真实 Secret、强杀不相关进程或依赖调用者当前目录。
- 为模块根包和关键边界包生成有实际内容的 `package-info.java`，说明职责、允许依赖和禁止依赖；
  不为占位而生成空 Javadoc。
- 创建建议的本地工作目录配置：`config/`、`browser/`、`projects/`、`artifacts/`、
  `resumes/`、`logs/`。仓库代码或用户项目源码不得复制到数据库中。

具体文件清单、日志滚动策略、Compose 安全基线和脚本行为见
[生产基础设施基线](references/production-foundation.md)。创建或补齐这些能力时必须读取并逐项验收。

### 3. API、日志与可观测性基础

- 保留 OpenAPI 接入：它用于生成和维护 RoleOS 对外 REST 接口的契约与文档，不是模型调用。
  在配置和文档中明确区分 `OpenAPI` 与 `OpenAI API`，避免把接口文档依赖误认为模型 Provider。
- 在 `roleos-web` 建立 `/api/v1` 前缀、统一成功响应与全局异常映射；仅提供基础健康或
  元信息端点，不抢先实现业务 API。
- 接入 OpenAPI 文档、Actuator、Micrometer 与 Prometheus 指标；暴露范围只包含必要的
  健康、信息、指标与 Prometheus 端点。
- 使用 SLF4J 与 `logback-spring.xml`；开发环境输出便于阅读的日志，生产环境输出结构化 JSON。
  日志配置必须包含 profile 分流、MDC 字段、异步输出、有界队列、丢弃/阻塞策略、滚动归档、
  单文件上限、保留天数、总容量上限和可配置日志级别。容器主输出保持 stdout/stderr；如启用文件
  日志，则写入受控 `ROLEOS_LOG_DIR`，不能依赖容器临时层永久保存。
- 统一传播并输出 `traceId`，业务上下文可按需加入 `workflowId`、`jobId` 和 `upgradeId`；在线程池、
  虚拟线程和异步边界验证 MDC 不丢失且请求结束后被清理。禁止 `System.out`，并保证日志不记录
  Secret、用户敏感职业信息、完整 JD/简历、完整外部会话或模型原始响应。
- 为 Logback 配置解析、profile 选择、JSON 必需字段、滚动策略、traceId 传播和脱敏增加自动测试；
  仅“应用能启动”不能证明日志配置可用于生产。
- 关键实现使用结构化事件日志，统一事件命名、字段、级别、异常记录、采样和脱敏规则；不得在每个
  方法机械打印 entry/exit。初始化日志 API 和 Review 门禁时读取
  [代码日志规范](references/code-logging.md)。

### 3.1 统一异常与安全响应基线

建立分层异常与错误响应基础，但不要预先定义 Job、Experience、Application 等领域业务错误码；
这些由对应 User Story 在拥有该规则的模块中补充。不要为了“统一”而让 Domain 依赖 Web，
也不要让所有异常继承一个携带 HTTP 状态的全局基类。

- 在低层模块定义框架无关的错误标识/异常，在 `roleos-web` 定义标准 API 错误响应和 HTTP
  映射。错误响应至少包含 `code`、面向用户的 `message`、`traceId` 与 `timestamp`；仅在安全的
  开发环境中才允许返回受限的诊断字段。初始化时若尚无跨模块消费者，错误契约留在最窄拥有者，
  不为未来假设提前创建 `core/base/common`。
- 使用 `@RestControllerAdvice` 统一映射参数校验、资源不存在、冲突、认证/授权失败、
  限流和未知异常到正确 HTTP 状态。未知异常只能返回通用错误码与 `traceId`，不得向客户端
  暴露堆栈、SQL、文件路径、Token、内部类名或上游 Provider 响应。
- 所有外部输入都必须在 Controller 边界进行 Bean Validation，并设置集合、字符串、分页、
  文件和 URL 的合理大小/格式限制。验证失败必须使用同一错误响应契约。
- 默认采用拒绝优先的 Web 安全配置：除明确的健康探针外，管理端点、API 文档和业务接口
  都必须有明确的访问策略；不得以 `permitAll`、`*` CORS 来源或关闭 CSRF/认证作为长期默认。
  认证方案未决时，应保留安全配置扩展点并在本地开发环境中显式说明例外范围。
- 对日志、异常消息、指标标签和审计事件应用脱敏规则。不得记录 Authorization Header、
  Cookie、API Key、密码、完整简历、完整 JD、完整 Agent Prompt 或模型原始响应。

### 3.2 密钥、数据与执行面安全

- Secret 只从环境变量、受控 Secret Store 或 OryxOS Credential/Secret Mechanism 读取；
  不写入 `application*.yml`、示例文件、测试夹具、日志、错误响应或 Git 历史。
- 添加 `.env.example`，只保留变量名和无效占位符；`.env`、本地密钥文件、工作目录产物和
  浏览器会话数据必须由 `.gitignore` 排除。
- 数据库连接使用最小权限账户；迁移权限与应用运行权限应可分离。生产配置必须强制 TLS、
  安全 Cookie 与可信代理设置由部署环境明确提供，不能依赖不安全默认值。
- `roleos-browser`、`roleos-codex` 和任何未来的文件/命令执行 Adapter 必须保留允许目录、
  规范化路径、命令参数化、外部 URL 协议/目标校验和审批边界；不得根据 Agent 自由文本
  拼接 Shell 命令或访问任意本地路径。
- Agent Runtime 与 Coding Agent 使用独立凭据作用域、最小工具权限、成本限额和 Trace。
  发送给模型的职业资料必须最小化并按脱敏策略处理；Career Fact 仍只写入 RoleOS 数据库。

### 4. Agent Runtime 边界

- 在 Domain 或 Application 定义稳定的 `AgentRuntimePort` 合约和结构化结果模型。
- 先实现 `FakeAgentRuntime`，使 Workflow 和契约测试无需真实 OryxOS 即可开发。
- 在 `roleos-runtime-oryx` 预留 OryxOS Adapter；仅在确认上游依赖坐标、许可证、版本兼容性
  和运行方式后，才接入真实 OryxOS。
- 职业对话、JD 分析和经历判断通过
  `AgentRuntimePort → OryxRuntimeAdapter → OryxOS → 模型 Provider/OpenAI API`；
  它与 `CodingAgentPort → CodexAdapter` 的代码执行链路是两条独立路径。即使底层模型
  Provider 相同，也要分别配置凭据作用域、权限、成本限额和 Trace。
- 若 OryxOS 不能稳定嵌入 JVM，保持 Port 不变，改用 REST/MCP Sidecar；不得因此改变
  Domain 模型或 Workflow。
- 所有 Agent 输出在写入持久化层前必须可进行 Schema 校验；不要在初始化阶段把 Agent
  Inference 写成 Career Fact。

### 5. 代码质量与安全门禁

代码注释必须解释契约、原因、约束、风险或不明显的决策，不能逐行翻译代码。公共 Port、Use Case、
领域规则、Workflow 状态转换、配置属性、外部 Adapter、安全边界和异常契约必须具备有意义的
Javadoc；简单 getter/setter、显然的构造器和自解释实现不强制注释。详细规则见
[代码注释规范](references/code-commenting.md)，初始化注释模板和质量门禁时必须读取。

在父 POM、编辑器配置和 CI 中配置一致的检查：

- Spotless + google-java-format：格式化、导入排序和未使用导入清理；
- Checkstyle 与 PMD：基础编码规范、公共契约 Javadoc、无效 TODO 和注释掉代码检查；
- Maven Javadoc/doclint：验证对外契约文档可生成；对生成代码、测试 Fixture 和框架要求的简单
  override 使用窄范围排除，不能全局关闭；
- SpotBugs + Find Security Bugs：常见缺陷与安全风险；
- OWASP Dependency-Check：第三方依赖已知漏洞；
- Gitleaks（或等效 Secret Scanner）：提交前和 CI 中扫描硬编码密钥与高风险凭据；
- JUnit 5 与 Testcontainers：单元、持久化和集成测试基础。

实现时先确认各插件与 Java 21、Spring Boot 基线兼容的稳定版本；在父 POM 统一锁定版本。
若某项检查产生大量与空骨架无关的误报，应记录原因并最小化配置，而不是直接关闭检查。

### 6. CI 与开发者体验

- 添加 `.editorconfig`、基础 `.gitignore`、格式化命令说明和贡献说明。
- 对 Shell 脚本运行 `shellcheck`（环境可用时）和最小 smoke test；对 Dockerfile/Compose 执行
  配置渲染、健康检查和容器退出行为验证。
- 在 CI 中检查公共边界 Javadoc、关键 `package-info.java`、TODO 格式和注释中的敏感信息；
  不设置按注释行数计算的覆盖率指标。
- 添加 GitHub Actions（或仓库指定 CI），在干净环境运行格式检查、静态分析、测试和依赖安全
  检查以及 Secret Scan。任一关键检查失败必须使 CI 失败。
- 提供 pre-commit 配置，至少执行快速格式、静态检查与 Secret Scan；不得要求开发者本地持有
  生产凭据。
- README 中仅补充实际可运行的本地启动、验证和配置说明，不把规划功能描述为已实现。

### 7. 验证与交付

按顺序执行并记录结果：

1. `mvn clean verify`；
2. 渲染并校验所有 Spring profile，验证生产必填配置缺失会 fail fast，启动摘要只包含允许字段；
3. 解析并测试 Logback 配置与关键代码日志，验证 JSON/MDC、事件字段、级别、异步输出、滚动保留
   和脱敏；
4. 对所有脚本做语法检查和 smoke test，使用脚本启动、查看状态/日志并停止一次；
5. 执行 `docker compose config`，启动 Compose，验证 RoleOS/PostgreSQL 健康、依赖顺序、持久卷、
   重启策略和日志容量限制，再执行幂等停止；
6. 启动 `roleos-boot`，验证健康、OpenAPI 和指标端点；
7. 验证 PostgreSQL/Flyway 基线及 Testcontainers 测试；
8. 对无效参数、未认证访问和未处理异常分别执行请求，确认状态码、统一错误码和 `traceId`
   正确，且响应中不含堆栈、路径、SQL、Token 或其他敏感信息；
9. 生成 Javadoc 并运行注释规范检查；用缺失公共契约说明、无任务号 TODO 和注释中的测试 Secret
   验证门禁能够阻断；
10. 故意引入一次格式问题和一次无效测试 Secret，确认质量与 Secret Scan 门禁会阻断；
11. 检查 Git diff，确认没有业务功能、明文 Secret、危险 CORS/管理端点暴露，或违反模块边界的依赖。

如果 Docker、OryxOS 或外部浏览器运行时不可用，应使用 Fake Adapter 完成可离线验证的部分，
并在最终报告中列出未执行项与原因；不得为了通过验证而伪造外部集成结果。

## 输出要求

完成后以中文报告：

1. 创建或修改的模块与基础设施；
2. 已执行的验证命令及结果；
3. 未执行的检查、阻塞原因和安全替代方案；
4. 与项目宪法或技术方案的任何冲突；
5. 下一步建议：使用 `$speckit-specify` 从 `US-1：Career Foundation + Durable Workflow`
   开始，而不是在初始化 Skill 中实现业务功能。

## 完成定义

- Maven 多模块骨架可构建，且依赖方向符合 RoleOS 分层。
- 分环境配置、关键配置 fail-fast 与脱敏启动摘要可验证，生产配置没有不安全回退值。
- 本地/生产日志 profile、JSON/MDC、异步与滚动保留策略可验证，日志不会泄露敏感信息。
- 关键实现日志使用统一事件名和结构化字段，级别准确且不存在无意义 entry/exit、重复异常或高基数
  敏感字段。
- 构建、验证、启动、停止、状态与日志脚本均已生成并通过 smoke test。
- Dockerfile 与 Compose 已初始化，RoleOS/PostgreSQL 的健康检查、持久化、网络、重启与日志限制
  可验证；可选 Runtime 使用显式 profile/override。
- 自定义异常、错误码和安全响应契约按层归属且可验证；默认响应不会泄露内部实现或敏感信息，
  未创建无边界的 `core/base/common` 杂物模块。
- 公共契约、模块边界和高风险逻辑具备有意义且可生成的 Javadoc；不存在机械逐行注释、无任务号
  TODO、注释掉的旧代码或注释中的敏感信息。
- OryxOS 仅通过 Port/Adapter 预留，Fake Runtime 可用于后续开发。
- 格式、静态分析、测试、依赖安全检查与 Secret Scan 已在本地或 CI 接入并能够阻断失败。
- 没有业务功能、明文密钥、虚构验证结果或对项目宪法的违规修改。
