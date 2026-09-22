/**
 * RoleOS 浏览器基础设施与招聘站点 Adapter。
 *
 * <p>本模块实现 {@code JobSiteAdapter -> BrowserRouter -> BrowserProvider} 边界。Provider 私有的会话、页面快照
 * 和元素引用不得越过本模块，也不得跨 Provider 复用；验证码与人工验证必须返回可持久化的人工暂停信号。
 */
package io.roleos.browser;
