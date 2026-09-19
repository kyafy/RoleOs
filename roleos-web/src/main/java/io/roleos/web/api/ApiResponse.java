package io.roleos.web.api;

import java.time.Instant;

/** RoleOS REST 接口的统一成功响应。 */
public record ApiResponse<T>(
    String code, String message, T data, String traceId, Instant timestamp) {

  public static <T> ApiResponse<T> success(T data, String traceId) {
    return new ApiResponse<>("SUCCESS", "请求成功", data, traceId, Instant.now());
  }
}
