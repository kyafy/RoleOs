package io.roleos.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.roleos.domain.career.UserId;
import io.roleos.web.error.RoleOsException;
import io.roleos.web.trace.TraceIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserResolverTest {

  private final CurrentUserResolver resolver = new CurrentUserResolver();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void rejectsUnauthenticatedRequest() {
    assertThatThrownBy(resolver::requireCurrentUser)
        .isInstanceOf(RoleOsException.class)
        .satisfies(
            exception -> assertThat(((RoleOsException) exception).status().value()).isEqualTo(401));
  }

  @Test
  void rejectsCrossUserResourceAccess() {
    UserId currentUser = UserId.random();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(currentUser.value().toString(), "N/A", "ROLE_USER"));

    assertThatThrownBy(() -> resolver.requireOwnership(UserId.random()))
        .isInstanceOf(RoleOsException.class)
        .satisfies(
            exception -> assertThat(((RoleOsException) exception).status().value()).isEqualTo(403));
  }

  @Test
  void writesTraceIdToResponse() throws Exception {
    TraceIdFilter filter = new TraceIdFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

    assertThat(response.getHeader(TraceIdFilter.HEADER_NAME)).isNotBlank();
  }
}
