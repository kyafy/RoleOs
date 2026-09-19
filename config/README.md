# 配置与密钥约定

运行时配置通过环境变量提供；仓库只保留不含真实凭据的 [`.env.example`](../.env.example)。

- 本地开发：复制 `.env.example` 为 `.env`，然后执行 `docker compose up -d postgres`。
- 密钥、令牌、私钥和生产数据库连接串不得写入 Java 源码、YAML、GitHub Actions 日志或提交记录。
- 生产环境由部署平台的 Secret Store 注入变量，并使用最小权限数据库账号与 TLS 连接。
- `ROLEOS_OPENAPI_ENABLED` 默认关闭；仅在受控本地环境显式开启 API 文档页面。

配置变更不得削弱默认拒绝访问、输入校验、日志脱敏或审计追踪等安全基线。
