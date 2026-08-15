package com.streamora.pet.infrastructure;

import com.streamora.pet.domain.ActivePet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** JDBC adapter for pet-service-owned personal pet instances. */
@Repository
public class PetInstanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public PetInstanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ActivePet findOrCreate(String subjectId, String displayName, String assetKey, Instant now) {
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO pet.pet_instance
                        (id, owner_subject_id, display_name, asset_key, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?)
                    """,
                    UUID.randomUUID().toString(),
                    subjectId,
                    displayName + "的伙伴",
                    assetKey,
                    Timestamp.from(now),
                    Timestamp.from(now));
        } catch (DuplicateKeyException ignored) {
            // A concurrent request or an existing user pet already owns this unique subject id.
        }
        return jdbcTemplate.query(
                        """
                        SELECT id, display_name, asset_key, owner_subject_id
                        FROM pet.pet_instance
                        WHERE owner_subject_id = ? AND status = 'ACTIVE'
                        """,
                        (resultSet, rowNumber) -> new ActivePet(
                                resultSet.getString("id"),
                                resultSet.getString("display_name"),
                                resultSet.getString("asset_key"),
                                "PERSONAL",
                                resultSet.getString("owner_subject_id")),
                        subjectId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Personal pet could not be loaded"));
    }
}
