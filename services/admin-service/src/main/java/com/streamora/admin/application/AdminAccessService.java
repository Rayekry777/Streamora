package com.streamora.admin.application;

import com.streamora.admin.domain.AdminAuthorization;
import com.streamora.admin.domain.AdminSession;
import com.streamora.admin.identity.AdminIdentityClient;
import com.streamora.admin.infrastructure.AdminRbacRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** Orchestrates admin identity, service-owned RBAC and immutable audit writes. */
@Service
public class AdminAccessService {

    private final AdminIdentityClient identityClient;
    private final AdminRbacRepository repository;
    private final Clock clock = Clock.systemUTC();

    public AdminAccessService(AdminIdentityClient identityClient, AdminRbacRepository repository) {
        this.identityClient = identityClient;
        this.repository = repository;
    }

    public AdminSession login(
            String login,
            String password,
            String userAgent,
            String ipAddress,
            String traceId) {
        var identity = identityClient.authenticate(login, password, userAgent, ipAddress, traceId)
                .orElseThrow(() -> new AdminAuthenticationException(
                        "INVALID_CREDENTIALS", "登录名或密码不正确"));
        AdminAuthorization authorization = repository.findAuthorization(identity.subjectId());
        if (!authorization.isAssigned()) {
            identityClient.revoke(identity.rawToken(), traceId);
            throw new AdminForbiddenException("ADMIN_ROLE_REQUIRED", "该账号未分配管理角色");
        }
        repository.appendAudit(
                identity.subjectId(), "ADMIN_LOGIN", "ADMIN_SESSION", identity.subjectId(), null, traceId, clock.instant());
        return new AdminSession(
                identity.rawToken(),
                identity.subjectId(),
                identity.displayName(),
                identity.expiresAt(),
                authorization);
    }

    public AdminSession resolve(String rawToken, String traceId) {
        var identity = identityClient.resolve(rawToken, traceId)
                .orElseThrow(() -> new AdminAuthenticationException(
                        "ADMIN_SESSION_REQUIRED", "管理员会话不存在或已过期"));
        AdminAuthorization authorization = repository.findAuthorization(identity.subjectId());
        if (!authorization.isAssigned()) {
            throw new AdminForbiddenException("ADMIN_ROLE_REQUIRED", "该账号未分配管理角色");
        }
        return new AdminSession(
                rawToken,
                identity.subjectId(),
                identity.displayName(),
                identity.expiresAt(),
                authorization);
    }

    public AdminSession requirePermission(String rawToken, String permission, String traceId) {
        AdminSession session = resolve(rawToken, traceId);
        if (!session.authorization().hasPermission(permission)) {
            throw new AdminForbiddenException("ADMIN_PERMISSION_DENIED", "当前角色缺少所需权限");
        }
        return session;
    }

    public void logout(String rawToken, String traceId) {
        AdminSession session = resolve(rawToken, traceId);
        if (!identityClient.revoke(rawToken, traceId)) {
            throw new AdminAuthenticationException("ADMIN_SESSION_REQUIRED", "管理员会话不存在或已过期");
        }
        repository.appendAudit(
                session.subjectId(), "ADMIN_LOGOUT", "ADMIN_SESSION", session.subjectId(), null, traceId, clock.instant());
    }
}
