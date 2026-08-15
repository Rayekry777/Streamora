package com.streamora.admin.config;

import com.streamora.admin.infrastructure.AdminRbacRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Optionally assigns SUPER_ADMIN to the separately created bootstrap identity. */
@Component
public class BootstrapAdminRoleInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapAdminRoleInitializer.class);

    private final AdminRbacRepository repository;
    private final String subjectId;

    public BootstrapAdminRoleInitializer(
            AdminRbacRepository repository,
            @Value("${streamora.bootstrap.admin-subject-id:}") String subjectId) {
        this.repository = repository;
        this.subjectId = subjectId;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        if (subjectId == null || subjectId.isBlank()) {
            return;
        }
        int assigned = repository.assignRoleIfMissing(
                subjectId.trim(), "SUPER_ADMIN", "SYSTEM_BOOTSTRAP", Clock.systemUTC().instant());
        if (assigned > 0) {
            LOGGER.info("Assigned configured bootstrap administrator role subjectId={}", subjectId.trim());
        }
    }
}
