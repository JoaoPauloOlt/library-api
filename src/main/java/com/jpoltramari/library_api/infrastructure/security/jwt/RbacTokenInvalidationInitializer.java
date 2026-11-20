package com.jpoltramari.library_api.infrastructure.security.jwt;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Installs PostgreSQL triggers that invalidate JWT authorization snapshots
 * whenever RBAC relationships or their names change.
 *
 * The statements are executed individually through JDBC because Spring's
 * schema.sql script parser uses semicolons as statement separators and cannot
 * safely parse PL/pgSQL function bodies.
 */
@Component
@DependsOnDatabaseInitialization
public class RbacTokenInvalidationInitializer {

    private static final List<String> DDL_STATEMENTS = List.of(
            """
            CREATE OR REPLACE FUNCTION invalidate_tokens_after_user_group_change()
            RETURNS TRIGGER
            LANGUAGE plpgsql
            AS $$
            BEGIN
                IF TG_OP = 'INSERT' THEN
                    UPDATE users SET token_version = token_version + 1
                    WHERE id = NEW.user_id;
                    RETURN NEW;
                ELSIF TG_OP = 'DELETE' THEN
                    UPDATE users SET token_version = token_version + 1
                    WHERE id = OLD.user_id;
                    RETURN OLD;
                ELSE
                    UPDATE users SET token_version = token_version + 1
                    WHERE id IN (OLD.user_id, NEW.user_id);
                    RETURN NEW;
                END IF;
            END;
            $$;
            """,
            """
            CREATE OR REPLACE FUNCTION invalidate_tokens_after_group_permission_change()
            RETURNS TRIGGER
            LANGUAGE plpgsql
            AS $$
            BEGIN
                IF TG_OP = 'INSERT' THEN
                    UPDATE users
                    SET token_version = token_version + 1
                    WHERE id IN (SELECT user_id FROM user_groups WHERE group_id = NEW.group_id);
                    RETURN NEW;
                ELSIF TG_OP = 'DELETE' THEN
                    UPDATE users
                    SET token_version = token_version + 1
                    WHERE id IN (SELECT user_id FROM user_groups WHERE group_id = OLD.group_id);
                    RETURN OLD;
                ELSE
                    UPDATE users
                    SET token_version = token_version + 1
                    WHERE id IN (
                        SELECT user_id
                        FROM user_groups
                        WHERE group_id IN (OLD.group_id, NEW.group_id)
                    );
                    RETURN NEW;
                END IF;
            END;
            $$;
            """,
            """
            CREATE OR REPLACE FUNCTION invalidate_tokens_after_group_rename()
            RETURNS TRIGGER
            LANGUAGE plpgsql
            AS $$
            BEGIN
                UPDATE users
                SET token_version = token_version + 1
                WHERE id IN (SELECT user_id FROM user_groups WHERE group_id = NEW.id);
                RETURN NEW;
            END;
            $$;
            """,
            """
            CREATE OR REPLACE FUNCTION invalidate_tokens_after_permission_rename()
            RETURNS TRIGGER
            LANGUAGE plpgsql
            AS $$
            BEGIN
                UPDATE users
                SET token_version = token_version + 1
                WHERE id IN (
                    SELECT ug.user_id
                    FROM user_groups ug
                    JOIN group_permissions gp ON gp.group_id = ug.group_id
                    WHERE gp.permission_id = NEW.id
                );
                RETURN NEW;
            END;
            $$;
            """,
            "DROP TRIGGER IF EXISTS trg_user_groups_token_invalidation ON user_groups",
            "DROP TRIGGER IF EXISTS trg_user_groups_token_invalidation_update ON user_groups",
            """
            CREATE TRIGGER trg_user_groups_token_invalidation
            AFTER INSERT OR DELETE ON user_groups
            FOR EACH ROW
            EXECUTE FUNCTION invalidate_tokens_after_user_group_change()
            """,
            """
            CREATE TRIGGER trg_user_groups_token_invalidation_update
            AFTER UPDATE OF user_id, group_id ON user_groups
            FOR EACH ROW
            WHEN (
                OLD.user_id IS DISTINCT FROM NEW.user_id
                OR OLD.group_id IS DISTINCT FROM NEW.group_id
            )
            EXECUTE FUNCTION invalidate_tokens_after_user_group_change()
            """,
            "DROP TRIGGER IF EXISTS trg_group_permissions_token_invalidation ON group_permissions",
            "DROP TRIGGER IF EXISTS trg_group_permissions_token_invalidation_update ON group_permissions",
            """
            CREATE TRIGGER trg_group_permissions_token_invalidation
            AFTER INSERT OR DELETE ON group_permissions
            FOR EACH ROW
            EXECUTE FUNCTION invalidate_tokens_after_group_permission_change()
            """,
            """
            CREATE TRIGGER trg_group_permissions_token_invalidation_update
            AFTER UPDATE OF group_id, permission_id ON group_permissions
            FOR EACH ROW
            WHEN (
                OLD.group_id IS DISTINCT FROM NEW.group_id
                OR OLD.permission_id IS DISTINCT FROM NEW.permission_id
            )
            EXECUTE FUNCTION invalidate_tokens_after_group_permission_change()
            """,
            "DROP TRIGGER IF EXISTS trg_groups_token_invalidation ON groups",
            """
            CREATE TRIGGER trg_groups_token_invalidation
            AFTER UPDATE OF name ON groups
            FOR EACH ROW
            WHEN (OLD.name IS DISTINCT FROM NEW.name)
            EXECUTE FUNCTION invalidate_tokens_after_group_rename()
            """,
            "DROP TRIGGER IF EXISTS trg_permissions_token_invalidation ON permissions",
            """
            CREATE TRIGGER trg_permissions_token_invalidation
            AFTER UPDATE OF name ON permissions
            FOR EACH ROW
            WHEN (OLD.name IS DISTINCT FROM NEW.name)
            EXECUTE FUNCTION invalidate_tokens_after_permission_rename()
            """
    );

    private final JdbcTemplate jdbcTemplate;

    public RbacTokenInvalidationInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        DDL_STATEMENTS.forEach(jdbcTemplate::execute);
    }
}
