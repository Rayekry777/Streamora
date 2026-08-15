package com.streamora.pet.identity;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Local fallback preserves the public mascot while granting no personal identity. */
@Configuration
public class UserIdentityClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserIdentityClient.class)
    UserIdentityClient anonymousOnlyIdentityClient() {
        return (rawToken, traceId) -> Optional.empty();
    }
}
