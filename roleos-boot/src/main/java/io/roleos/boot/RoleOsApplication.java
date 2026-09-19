package io.roleos.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** RoleOS 模块化单体的启动入口。 */
@SpringBootApplication(scanBasePackages = "io.roleos")
public class RoleOsApplication {

  public static void main(String[] args) {
    SpringApplication.run(RoleOsApplication.class, args);
  }
}
