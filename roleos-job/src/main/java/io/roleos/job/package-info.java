/**
 * Job Intelligence 的领域模型、端口与确定性用例。
 *
 * <p>本模块只依赖稳定领域原语和由自身拥有的 Port。禁止引用 Spring Web、数据库实现、MCP、 Playwright、Kimi、CSS/XPath
 * 或任何浏览器会话/页面句柄。岗位事实、候选策略和用户决策必须保持分离。
 */
package io.roleos.job;
