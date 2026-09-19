package io.roleos.web.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RoleOS 自身 REST OpenAPI 文档基础信息；它不是 OpenAI 模型 API 配置。 */
@Configuration
public class OpenApiConfiguration {

  @Bean
  OpenAPI roleOsOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("RoleOS REST API")
                .version("v1")
                .description("RoleOS 对外 REST 接口契约。默认安全策略拒绝未授权访问。"));
  }
}
