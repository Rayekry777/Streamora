package com.streamora.identity.infrastructure;

import com.streamora.identity.domain.SessionAudience;
import com.streamora.identity.domain.SessionPrincipal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** JDBC adapter for identity-owned accounts, credentials and isolated sessions. */
@Repository
public class IdentityJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public IdentityJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createAccount(
            String login,
            String displayName,
            SessionAudience accountType,
            String passwordHash,
            Instant now) {
        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO identity.account
                        (login, display_name, account_type, status, created_at)
                    VALUES (?, ?, ?, 'ACTIVE', ?)
                    """,
                    new String[] {"id"});
            statement.setString(1, login);
            statement.setString(2, displayName);
            statement.setString(3, accountType.name());
            statement.setTimestamp(4, Timestamp.from(now));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Account insert did not return an identifier");
        }
        long accountId = key.longValue();
        jdbcTemplate.update(
                """
                INSERT INTO identity.credential (account_id, password_hash, password_changed_at)
                VALUES (?, ?, ?)
                """,
                accountId,
                passwordHash,
                Timestamp.from(now));
        return accountId;
    }

    public Optional<AccountCredential> findCredential(String login, SessionAudience accountType) {
        return jdbcTemplate.query(
                        """
                        SELECT a.id, a.display_name, a.status, c.password_hash
                        FROM identity.account a
                        JOIN identity.credential c ON c.account_id = a.id
                        WHERE a.login = ? AND a.account_type = ?
                        """,
                        (resultSet, rowNumber) -> new AccountCredential(
                                resultSet.getLong("id"),
                                resultSet.getString("display_name"),
                                resultSet.getString("status"),
                                resultSet.getString("password_hash")),
                        login,
                        accountType.name())
                .stream()
                .findFirst();
    }

    public boolean accountExists(String login, SessionAudience accountType) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity.account WHERE login = ? AND account_type = ?",
                Integer.class,
                login,
                accountType.name());
        return count != null && count > 0;
    }

    public void createSession(
            String sessionId,
            long accountId,
            String tokenHash,
            SessionAudience audience,
            Instant now,
            Instant expiresAt) {
        jdbcTemplate.update(
                "INSERT INTO identity.%s_session (id, account_id, token_hash, expires_at, created_at) VALUES (?, ?, ?, ?, ?)"
                        .formatted(audience.name().toLowerCase(Locale.ROOT)),
                sessionId,
                accountId,
                tokenHash,
                Timestamp.from(expiresAt),
                Timestamp.from(now));
    }

    public Optional<SessionPrincipal> findActiveSession(
            String tokenHash,
            SessionAudience audience,
            Instant now) {
        String table = audience.name().toLowerCase(Locale.ROOT) + "_session";
        return jdbcTemplate.query(
                        """
                        SELECT a.id, a.display_name, s.expires_at
                        FROM identity.%s s
                        JOIN identity.account a ON a.id = s.account_id
                        WHERE s.token_hash = ?
                          AND s.revoked_at IS NULL
                          AND s.expires_at > ?
                          AND a.account_type = ?
                          AND a.status = 'ACTIVE'
                        """.formatted(table),
                        (resultSet, rowNumber) -> new SessionPrincipal(
                                resultSet.getLong("id"),
                                resultSet.getString("display_name"),
                                audience,
                                resultSet.getTimestamp("expires_at").toInstant()),
                        tokenHash,
                        Timestamp.from(now),
                        audience.name())
                .stream()
                .findFirst();
    }

    public int revokeSession(String tokenHash, SessionAudience audience, Instant now) {
        String table = audience.name().toLowerCase(Locale.ROOT) + "_session";
        return jdbcTemplate.update(
                "UPDATE identity.%s SET revoked_at = ? WHERE token_hash = ? AND revoked_at IS NULL"
                        .formatted(table),
                Timestamp.from(now),
                tokenHash);
    }

    public record AccountCredential(long id, String displayName, String status, String passwordHash) {
    }
}
