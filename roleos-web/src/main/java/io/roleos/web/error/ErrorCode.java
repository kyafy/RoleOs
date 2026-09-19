package io.roleos.web.error;

/** 对外稳定的错误码；不得将内部异常、SQL、路径或堆栈暴露给客户端。 */
public enum ErrorCode {
  VALIDATION_ERROR,
  AUTHENTICATION_REQUIRED,
  ACCESS_DENIED,
  RESOURCE_NOT_FOUND,
  CONFLICT,
  RATE_LIMITED,
  INTERNAL_ERROR
}
