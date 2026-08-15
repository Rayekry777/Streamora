package com.streamora.media.identity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MediaUserIdentityClientConfiguration {
    @Bean
    @ConditionalOnMissingBean(MediaUserIdentityClient.class)
    MediaUserIdentityClient anonymousIdentityClient() {
        return (rawToken, traceId) -> java.util.Optional.empty();
    }
}
