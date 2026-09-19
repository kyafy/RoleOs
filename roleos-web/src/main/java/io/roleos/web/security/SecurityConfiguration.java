package io.roleos.web.security;

import io.roleos.web.error.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 默认拒绝所有业务接口的安全基线。
 *
 * <p>仅健康检查可匿名访问；新增公开端点必须在评审中明确登记。
 */
@Configuration
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorWriter errorWriter)
      throws Exception {
    return http.authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/api/v1/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
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
  }
}
