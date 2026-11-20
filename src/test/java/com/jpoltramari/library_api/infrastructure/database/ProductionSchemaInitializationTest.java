package com.jpoltramari.library_api.infrastructure.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=${PROD_SCHEMA_TEST_DB_URL:jdbc:postgresql://localhost:5432/library_schema}",
        "spring.datasource.username=${DB_USER:postgres}",
        "spring.datasource.password=${DB_PASS:postgres}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.defer-datasource-initialization=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql",
        "spring.sql.init.data-locations=classpath:data.sql",
        "jwt.secret=${JWT_SECRET:library-api-prod-schema-test-secret-2026}",
        "jwt.expiration=${JWT_EXPIRATION:3600000}",
        "jwt.refresh-expiration=${JWT_REFRESH_EXPIRATION:86400000}",
        "jwt.issuer=${JWT_ISSUER:library-api}",
        "jwt.audience=${JWT_AUDIENCE:library-api-clients}",
        "app.cors.allowed-origins=http://localhost:3000"
})
@ActiveProfiles("prod")
class ProductionSchemaInitializationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldProvisionFreshProductionSchemaAndSeedRbacBeforeHibernateValidation() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                      'users',
                      'groups',
                      'permissions',
                      'group_permissions',
                      'user_groups',
                      'authors',
                      'books',
                      'book_authors',
                      'book_copies',
                      'loans',
                      'refresh_tokens'
                  )
                """, Integer.class);

        assertThat(tableCount).isEqualTo(11);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM groups", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class)).isGreaterThan(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();

        String copyStatusConstraint = jdbcTemplate.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'chk_book_copy_status'",
                String.class
        );
        assertThat(copyStatusConstraint).contains("'RESERVED'");
    }
    @Test
    void shouldMigrateLegacyNullBookCopyUpdatedAtValues() {
        jdbcTemplate.execute("ALTER TABLE book_copies ALTER COLUMN updated_at DROP NOT NULL");

        Long bookId = jdbcTemplate.queryForObject("""
                INSERT INTO books (isbn, title, genre, description)
                VALUES ('9780000000001', 'Migration fixture', 'CLASSIC', 'Migration fixture')
                RETURNING id
                """, Long.class);

        Long copyId = jdbcTemplate.queryForObject("""
                INSERT INTO book_copies (book_id, barcode, status, updated_at)
                VALUES (?, 'MIGRATION-FIXTURE-001', 'AVAILABLE', NULL)
                RETURNING id
                """, Long.class, bookId);

        new ResourceDatabasePopulator(new ClassPathResource(
                "db/manual-migrations/2026-11-20-book-copy-updated-at-not-null.sql"
        )).execute(dataSource);

        String nullable = jdbcTemplate.queryForObject("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'book_copies'
                  AND column_name = 'updated_at'
                """, String.class);
        String defaultValue = jdbcTemplate.queryForObject("""
                SELECT column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'book_copies'
                  AND column_name = 'updated_at'
                """, String.class);
        Integer backfilled = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM book_copies
                WHERE id = ?
                  AND updated_at IS NOT NULL
                  AND updated_at = created_at
                """, Integer.class, copyId);
        Integer recorded = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM schema_migrations
                WHERE version = '2026-11-20-book-copy-updated-at-not-null'
                """, Integer.class);

        assertThat(nullable).isEqualTo("NO");
        assertThat(defaultValue).contains("CURRENT_TIMESTAMP");
        assertThat(backfilled).isEqualTo(1);
        assertThat(recorded).isEqualTo(1);
    }

}
