package com.jpoltramari.library_api.infrastructure.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
        "jwt.secret=${JWT_SECRET:bGlicmFyeS1hcGktc2VjcmV0LXRlc3QtMjAyNi0wOC0yNA==}",
        "jwt.expiration=${JWT_EXPIRATION:3600000}",
        "jwt.refresh-expiration=${JWT_REFRESH_EXPIRATION:86400000}",
        "jwt.issuer=${JWT_ISSUER:library-api}",
        "jwt.audience=${JWT_AUDIENCE:library-api-clients}"
})
@ActiveProfiles("prod")
class ProductionSchemaInitializationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM groups", Integer.class))
                .isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class))
                .isGreaterThan(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class))
                .isZero();
    }
}
