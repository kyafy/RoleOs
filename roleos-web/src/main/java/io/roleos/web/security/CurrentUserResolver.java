package io.roleos.web.security;

import io.roleos.domain.career.UserId;
import io.roleos.web.error.ErrorCode;
import io.roleos.web.error.RoleOsException;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 从已认证主体解析职业数据归属用户。
 *
 * <p>此类刻意不接收 HTTP 请求体、查询参数或路径参数，防止客户端伪造或覆盖 userId。
 */
@Component
@SuppressWarnings("PMD.PreserveStackTrace")
public final class CurrentUserResolver {

  public UserId requireCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new RoleOsException(
          ErrorCode.AUTHENTICATION_REQUIRED, HttpStatus.UNAUTHORIZED, "需要完成身份认证");
    }

    try {
      return new UserId(UUID.fromString(authentication.getName()));
    } catch (IllegalArgumentException exception) {
      throw new RoleOsException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, "认证主体无有效用户标识");
    }
  }

  /** 确认当前认证主体是待访问职业资源的所有者。 */
  public UserId requireOwnership(UserId resourceOwner) {
    Objects.requireNonNull(resourceOwner, "资源归属用户不能为空");
    UserId currentUser = requireCurrentUser();
    if (!currentUser.equals(resourceOwner)) {
      throw new RoleOsException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, "当前身份无权访问该资源");
    }
    return currentUser;
  }
}
