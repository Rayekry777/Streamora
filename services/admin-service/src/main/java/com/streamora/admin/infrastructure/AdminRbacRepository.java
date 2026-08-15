package com.streamora.admin.infrastructure;

import com.streamora.admin.domain.AdminAuthorization;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** JDBC adapter for admin-owned RBAC assignments and append-only audit records. */
@Repository
public class AdminRbacRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminRbacRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminAuthorization findAuthorization(String subjectId) {
        var roles = new LinkedHashSet<>(jdbcTemplate.queryForList(
                """
                SELECT r.code FROM admin.subject_role sr
                JOIN admin.role r ON r.id = sr.role_id
                WHERE sr.subject_id = ? ORDER BY r.code
                """,
                String.class,
                subjectId));
        var permissions = new LinkedHashSet<>(jdbcTemplate.queryForList(
                """
                SELECT DISTINCT p.code FROM admin.subject_role sr
                JOIN admin.role_permission rp ON rp.role_id = sr.role_id
                JOIN admin.permission p ON p.id = rp.permission_id
                WHERE sr.subject_id = ? ORDER BY p.code
                """,
                String.class,
                subjectId));
        return new AdminAuthorization(Set.copyOf(roles), Set.copyOf(permissions));
    }

    public int assignRoleIfMissing(String subjectId, String roleCode, String assignedBy, Instant now) {
        return jdbcTemplate.update(
                """
                INSERT INTO admin.subject_role(subject_id, role_id, assigned_at, assigned_by)
                SELECT ?, r.id, ?, ? FROM admin.role r
                WHERE r.code = ? AND NOT EXISTS (
                    SELECT 1 FROM admin.subject_role sr
                    WHERE sr.subject_id = ? AND sr.role_id = r.id
                )
                """,
                subjectId,
                Timestamp.from(now),
                assignedBy,
                roleCode,
                subjectId);
    }

    public void appendAudit(
            String subjectId,
            String action,
            String targetType,
            String targetId,
            String reason,
            String traceId,
            Instant now) {
        jdbcTemplate.update(
                """
                INSERT INTO admin.audit_log
                    (id, subject_id, action, target_type, target_id, reason, trace_id, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID().toString(),
                subjectId,
                action,
                targetType,
                targetId,
                reason,
                traceId,
                Timestamp.from(now));
    }
}
