package com.streamora.admin.identity;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Keeps local health startup independent from infrastructure while never faking authentication. */
@Configuration
public class AdminIdentityClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(AdminIdentityClient.class)
    AdminIdentityClient unavailableAdminIdentityClient() {
        return new AdminIdentityClient() {
            @Override
            public Optional<AuthenticatedAdmin> authenticate(
                    String login, String password, String userAgent, String ipAddress, String traceId) {
                throw new IdentityUnavailableException("identity-service RPC 未启用");
            }

            @Override
            public Optional<AdminPrincipal> resolve(String rawToken, String traceId) {
                throw new IdentityUnavailableException("identity-service RPC 未启用");
            }

            @Override
            public boolean revoke(String rawToken, String traceId) {
                throw new IdentityUnavailableException("identity-service RPC 未启用");
            }
        };
    }
}
