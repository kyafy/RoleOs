package io.roleos.web.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 在请求、日志及响应头间传递可安全记录的 traceId。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class TraceIdFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-Trace-Id";
  public static final String MDC_KEY = "traceId";
  private static final int MAX_TRACE_ID_LENGTH = 128;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = resolveTraceId(request.getHeader(HEADER_NAME));
    MDC.put(MDC_KEY, traceId);
    response.setHeader(HEADER_NAME, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  private String resolveTraceId(String candidate) {
    if (candidate != null
        && candidate.length() <= MAX_TRACE_ID_LENGTH
        && candidate.matches("[A-Za-z0-9._-]+")) {
      return candidate;
    }
    return UUID.randomUUID().toString();
  }
}
