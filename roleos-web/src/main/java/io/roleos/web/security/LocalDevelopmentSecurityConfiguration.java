package io.roleos.web.security;

import io.roleos.web.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 仅限本机验收的 HTTP Basic 身份入口；用户名必须是 UUID，生产 Profile 永不加载此配置。
 *
 * <p>用户名和密码均由环境变量提供，禁止提交默认口令、令牌或真实账户信息。
 */
@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalDevelopmentSecurityConfiguration {

  @Bean
  UserDetailsService localUserDetailsService(
      @Value("${roleos.local-auth.user-id}") String userId,
      @Value("${roleos.local-auth.password}") String password) {
    return new InMemoryUserDetailsManager(
        User.withUsername(userId).password("{noop}" + password).roles("USER").build());
  }

  @Bean
  SecurityFilterChain localSecurityFilterChain(HttpSecurity http, SecurityErrorWriter errorWriter) {
    try {
      return http.authorizeHttpRequests(
              authorization ->
                  authorization
                      .requestMatchers("/actuator/health", "/actuator/health/**")
                      .permitAll()
                      .requestMatchers("/api/**", "/jobs", "/jobs/**")
                      .authenticated()
                      .anyRequest()
                      .denyAll())
          .httpBasic(Customizer.withDefaults())
          .exceptionHandling(
              exceptions ->
                  exceptions
                      .authenticationEntryPoint(
                          (request, response, exception) ->
                              errorWriter.write(
                                  request,
                                  response,
                                  HttpStatus.UNAUTHORIZED.value(),
                                  ErrorCode.AUTHENTICATION_REQUIRED,
                                  "需要完成身份认证"))
                      .accessDeniedHandler(
                          (request, response, exception) ->
                              errorWriter.write(
                                  request,
                                  response,
                                  HttpStatus.FORBIDDEN.value(),
                                  ErrorCode.ACCESS_DENIED,
                                  "当前身份无权访问该资源")))
          .csrf(Customizer.withDefaults())
          .build();
    } catch (Exception exception) {
      throw new IllegalStateException("无法初始化本地安全过滤器链", exception);
    }
  }
}
