package io.roleos.web.api.career;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Career API 所需的确定性基础依赖。 */
@Configuration
class CareerApiConfiguration {
  @Bean
  Clock careerClock() {
    return Clock.systemUTC();
  }
}
