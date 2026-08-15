package com.streamora.pet.identity;

import com.streamora.contract.identity.v1.IdentitySessionService;
import com.streamora.contract.identity.v1.SessionTokenQuery;
import java.util.Optional;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Dubbo Triple identity adapter that always requests the USER audience. */
@Component
@ConditionalOnProperty(name = "streamora.identity.rpc-enabled", havingValue = "true")
public class DubboUserIdentityClient implements UserIdentityClient {

    @DubboReference(interfaceClass = IdentitySessionService.class, version = "1.0.0", check = false, timeout = 3000)
    private IdentitySessionService identitySessionService;

    @Override
    public Optional<UserPrincipal> resolveUser(String rawToken, String traceId) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        var result = identitySessionService.resolveUserSession(SessionTokenQuery.newBuilder()
                .setRawToken(rawToken)
                .setExpectedAudience("USER")
                .setTraceId(traceId)
                .build());
        if (!result.getAuthenticated() || !"USER".equals(result.getAudience())) {
            return Optional.empty();
        }
        return Optional.of(new UserPrincipal(result.getSubjectId(), result.getDisplayName()));
    }
}
