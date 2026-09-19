package io.roleos.web.error;

import io.roleos.web.trace.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将可预期错误与未知错误统一转换为不泄露内部细节的 API 契约。 */
@RestControllerAdvice
public final class GlobalExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(RoleOsException.class)
  public ResponseEntity<ApiErrorResponse> handleRoleOsException(
      RoleOsException exception, HttpServletRequest request) {
    return ResponseEntity.status(exception.status())
        .body(ApiErrorResponse.of(exception.errorCode(), exception.getMessage(), traceId(request)));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationException(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, "请求参数校验失败", traceId(request)));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnknownException(
      Exception exception, HttpServletRequest request) {
    String traceId = traceId(request);
    LOGGER.error("未处理的请求异常，traceId={}", traceId, exception);
    return ResponseEntity.internalServerError()
        .body(ApiErrorResponse.of(ErrorCode.INTERNAL_ERROR, "服务暂时不可用，请稍后重试", traceId));
  }

  private String traceId(HttpServletRequest request) {
    return Objects.requireNonNullElse(request.getHeader(TraceIdFilter.HEADER_NAME), "unknown");
  }
}
