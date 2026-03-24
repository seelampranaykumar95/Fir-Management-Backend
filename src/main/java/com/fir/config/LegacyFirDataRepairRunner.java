package com.fir.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name = "app.legacy-fir-data-repair.enabled", havingValue = "true", matchIfMissing = true)
public class LegacyFirDataRepairRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(LegacyFirDataRepairRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public LegacyFirDataRepairRunner(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(status -> repairLegacyFirData());
    }

    private void repairLegacyFirData() {
        int normalizedPending = jdbcTemplate.update("""
                UPDATE firs
                SET status = 'PENDING_REVIEW'
                WHERE status = 'PENDING'
                """);

        int normalizedInvalidStatuses = jdbcTemplate.update("""
                UPDATE firs
                SET status = 'SUBMITTED'
                WHERE status IS NULL
                   OR TRIM(status) = ''
                   OR status NOT IN (
                       'SUBMITTED',
                       'PENDING_REVIEW',
                       'NEEDS_INFO',
                       'ACCEPTED',
                       'ASSIGNED',
                       'INVESTIGATION',
                       'CLOSED',
                       'REJECTED'
                   )
                """);

        int normalizedCategories = jdbcTemplate.update("""
                UPDATE firs
                SET category = 'OTHER'
                WHERE category IS NULL
                   OR category = ''
                """);

        int clearedMissingStations = jdbcTemplate.update("""
                UPDATE firs f
                LEFT JOIN police_stations ps ON ps.id = f.police_station_id
                SET f.police_station_id = NULL
                WHERE f.police_station_id IS NOT NULL
                  AND ps.id IS NULL
                """);

        int deletedOrphanAssignments = jdbcTemplate.update("""
                DELETE fa
                FROM fir_assignments fa
                JOIN firs f ON f.id = fa.fir_id
                LEFT JOIN users u ON u.id = f.filed_by_user_id
                WHERE f.filed_by_user_id IS NOT NULL
                  AND u.id IS NULL
                """);

        int deletedOrphanUpdates = jdbcTemplate.update("""
                DELETE fu
                FROM fir_updates fu
                JOIN firs f ON f.id = fu.fir_id
                LEFT JOIN users u ON u.id = f.filed_by_user_id
                WHERE f.filed_by_user_id IS NOT NULL
                  AND u.id IS NULL
                """);

        int deletedOrphanEvidence = jdbcTemplate.update("""
                DELETE ef
                FROM evidence_files ef
                JOIN firs f ON f.id = ef.fir_id
                LEFT JOIN users u ON u.id = f.filed_by_user_id
                WHERE f.filed_by_user_id IS NOT NULL
                  AND u.id IS NULL
                """);

        int deletedOrphanFirs = jdbcTemplate.update("""
                DELETE f
                FROM firs f
                LEFT JOIN users u ON u.id = f.filed_by_user_id
                WHERE f.filed_by_user_id IS NOT NULL
                  AND u.id IS NULL
                """);

        int totalChanges = normalizedPending
                + normalizedInvalidStatuses
                + normalizedCategories
                + clearedMissingStations
                + deletedOrphanAssignments
                + deletedOrphanUpdates
                + deletedOrphanEvidence
                + deletedOrphanFirs;

        if (totalChanges > 0) {
            logger.info(
                    "Repaired legacy FIR data on startup: pending={}, invalidStatuses={}, categories={}, clearedStations={}, deletedAssignments={}, deletedUpdates={}, deletedEvidence={}, deletedFirs={}",
                    normalizedPending,
                    normalizedInvalidStatuses,
                    normalizedCategories,
                    clearedMissingStations,
                    deletedOrphanAssignments,
                    deletedOrphanUpdates,
                    deletedOrphanEvidence,
                    deletedOrphanFirs);
        }
    }
}

