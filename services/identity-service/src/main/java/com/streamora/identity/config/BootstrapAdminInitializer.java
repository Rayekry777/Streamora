package com.streamora.identity.config;

import com.streamora.identity.application.IdentityAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Optional local bootstrap controlled entirely by runtime environment values. */
@Component
public class BootstrapAdminInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final IdentityAuthService authService;
    private final String login;
    private final String password;
    private final String displayName;

    public BootstrapAdminInitializer(
            IdentityAuthService authService,
            @Value("${streamora.bootstrap.admin-login:}") String login,
            @Value("${streamora.bootstrap.admin-password:}") String password,
            @Value("${streamora.bootstrap.admin-display-name:Bootstrap Administrator}") String displayName) {
        this.authService = authService;
        this.login = login;
        this.password = password;
        this.displayName = displayName;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        authService.createBootstrapAdmin(login, displayName, password)
                .ifPresent(subjectId -> LOGGER.info("Created configured bootstrap administrator subjectId={}", subjectId));
    }
}
