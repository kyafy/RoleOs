package io.roleos.web.error;

import java.time.Instant;

/** RoleOS REST 接口的统一失败响应。 */
public record ApiErrorResponse(ErrorCode code, String message, String traceId, Instant timestamp) {

  public static ApiErrorResponse of(ErrorCode code, String message, String traceId) {
    return new ApiErrorResponse(code, message, traceId, Instant.now());
  }
}
