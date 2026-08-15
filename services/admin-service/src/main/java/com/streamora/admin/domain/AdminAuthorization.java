package com.streamora.admin.domain;

import java.util.Set;

/** Roles and permissions owned by admin-service for one identity subject. */
public record AdminAuthorization(Set<String> roles, Set<String> permissions) {

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean isAssigned() {
        return !roles.isEmpty();
    }
}
