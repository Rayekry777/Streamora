package com.streamora.identity.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Runtime policy for isolated user and administrator sessions. */
@Component
@ConfigurationProperties(prefix = "streamora.session")
public class IdentitySessionProperties {

    private boolean secureCookie;
    private Duration userDuration = Duration.ofHours(8);
    private Duration adminDuration = Duration.ofHours(2);

    public boolean isSecureCookie() {
        return secureCookie;
    }

    public void setSecureCookie(boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    public Duration getUserDuration() {
        return userDuration;
    }

    public void setUserDuration(Duration userDuration) {
        this.userDuration = userDuration;
    }

    public Duration getAdminDuration() {
        return adminDuration;
    }

    public void setAdminDuration(Duration adminDuration) {
        this.adminDuration = adminDuration;
    }
}
