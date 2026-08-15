package com.streamora.admin.identity;

import com.streamora.contract.identity.v1.AdminLoginCommand;
import com.streamora.contract.identity.v1.IdentitySessionService;
import com.streamora.contract.identity.v1.RevokeSessionCommand;
import com.streamora.contract.identity.v1.SessionTokenQuery;
import java.time.Instant;
import java.util.Optional;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Dubbo Triple adapter for the protobuf identity session contract. */
@Component
@ConditionalOnProperty(name = "streamora.identity.rpc-enabled", havingValue = "true")
public class DubboAdminIdentityClient implements AdminIdentityClient {

    @DubboReference(interfaceClass = IdentitySessionService.class, version = "1.0.0", check = false, timeout = 3000)
    private IdentitySessionService identitySessionService;

    @Override
    public Optional<AuthenticatedAdmin> authenticate(
            String login, String password, String userAgent, String ipAddress, String traceId) {
        try {
            var result = identitySessionService.authenticateAdmin(AdminLoginCommand.newBuilder()
                    .setLogin(login)
                    .setPassword(password)
                    .setUserAgent(userAgent)
                    .setIpAddress(ipAddress)
                    .setTraceId(traceId)
                    .build());
            if (!result.getAuthenticated()) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedAdmin(
                    result.getRawToken(),
                    result.getSubjectId(),
                    result.getDisplayName(),
                    Instant.parse(result.getExpiresAt())));
        } catch (RuntimeException exception) {
            throw new IdentityUnavailableException("identity-service 管理员认证调用失败", exception);
        }
    }

    @Override
    public Optional<AdminPrincipal> resolve(String rawToken, String traceId) {
        try {
            var result = identitySessionService.resolveAdminSession(SessionTokenQuery.newBuilder()
                    .setRawToken(rawToken == null ? "" : rawToken)
                    .setExpectedAudience("ADMIN")
                    .setTraceId(traceId)
                    .build());
            if (!result.getAuthenticated() || !"ADMIN".equals(result.getAudience())) {
                return Optional.empty();
            }
            return Optional.of(new AdminPrincipal(
                    result.getSubjectId(), result.getDisplayName(), Instant.parse(result.getExpiresAt())));
        } catch (RuntimeException exception) {
            throw new IdentityUnavailableException("identity-service 管理员会话解析调用失败", exception);
        }
    }

    @Override
    public boolean revoke(String rawToken, String traceId) {
        try {
            return identitySessionService.revokeAdminSession(RevokeSessionCommand.newBuilder()
                            .setRawToken(rawToken == null ? "" : rawToken)
                            .setExpectedAudience("ADMIN")
                            .setTraceId(traceId)
                            .build())
                    .getCompleted();
        } catch (RuntimeException exception) {
            throw new IdentityUnavailableException("identity-service 管理员会话撤销调用失败", exception);
        }
    }
}
