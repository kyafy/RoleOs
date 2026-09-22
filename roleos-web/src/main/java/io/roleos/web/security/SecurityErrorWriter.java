package io.roleos.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.roleos.web.error.ApiErrorResponse;
import io.roleos.web.error.ErrorCode;
import io.roleos.web.trace.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** 认证与授权失败的统一响应写入器。 */
@Component
public final class SecurityErrorWriter {

  private static final ObjectWriter JSON_WRITER =
      new ObjectMapper().registerModule(new JavaTimeModule()).writer();

  public void write(
      HttpServletRequest request,
      HttpServletResponse response,
      int status,
      ErrorCode errorCode,
      String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    JSON_WRITER.writeValue(
        response.getOutputStream(),
        ApiErrorResponse.of(
            errorCode,
            message,
            Objects.requireNonNullElse(request.getHeader(TraceIdFilter.HEADER_NAME), "unknown")));
  }
}
