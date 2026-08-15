package com.streamora.admin.api;

import com.streamora.admin.application.AdminAccessService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** First RBAC-protected admin endpoint used by the phase-two management shell. */
@RestController
@RequestMapping("/admin-api/v1/operations")
public class AdminOperationsController {

    private final AdminAccessService accessService;

    public AdminOperationsController(AdminAccessService accessService) {
        this.accessService = accessService;
    }

    @GetMapping("/overview")
    public ApiEnvelope<OperationsOverviewView> overview(
            @CookieValue(value = AdminAuthController.SESSION_COOKIE, required = false) String rawToken,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String traceId = RequestIds.resolve(requestId);
        accessService.requirePermission(rawToken, "DASHBOARD_VIEW", traceId);
        return new ApiEnvelope<>(new OperationsOverviewView("2", "IN_PROGRESS"), traceId);
    }

    public record OperationsOverviewView(String phase, String status) {
    }
}
