package io.roleos.web.error;

import java.util.Objects;
import org.springframework.http.HttpStatus;

/** 业务层可显式抛出的安全异常基类。 */
public class RoleOsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final ErrorCode code;
  private final HttpStatus statusValue;

  public RoleOsException(ErrorCode errorCode, HttpStatus status, String message) {
    super(message);
    this.code = Objects.requireNonNull(errorCode, "errorCode 不能为空");
    this.statusValue = Objects.requireNonNull(status, "status 不能为空");
  }

  public RoleOsException(ErrorCode errorCode, HttpStatus status, String message, Throwable cause) {
    super(message, cause);
    this.code = Objects.requireNonNull(errorCode, "errorCode 不能为空");
    this.statusValue = Objects.requireNonNull(status, "status 不能为空");
  }

  public ErrorCode errorCode() {
    return code;
  }

  public HttpStatus status() {
    return statusValue;
  }
}
