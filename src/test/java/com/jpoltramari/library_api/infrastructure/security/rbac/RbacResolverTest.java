package com.jpoltramari.library_api.infrastructure.security.rbac;

import com.jpoltramari.library_api.domain.model.Group;
import com.jpoltramari.library_api.domain.model.Permission;
import com.jpoltramari.library_api.domain.model.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RbacResolverTest {

    private final RbacResolver resolver = new RbacResolver();

    @Test
    void shouldResolveGroupsInDeterministicOrder() {
        User user = userWithGroups("USER", "ADMIN", "LIBRARIAN");

        assertThat(resolver.resolveGroups(user))
                .containsExactly("ADMIN", "LIBRARIAN", "USER");
    }

    @Test
    void shouldFlattenAndDeduplicatePermissionsAcrossGroups() {
        User user = userWithGroups("USER", "LIBRARIAN");
        Group userGroup = user.getGroups().stream()
                .filter(group -> "USER".equals(group.getName()))
                .findFirst()
                .orElseThrow();
        Group librarianGroup = user.getGroups().stream()
                .filter(group -> "LIBRARIAN".equals(group.getName()))
                .findFirst()
                .orElseThrow();

        userGroup.getPermissions().add(permission("BOOK_READ"));
        librarianGroup.getPermissions().add(permission("BOOK_READ"));
        librarianGroup.getPermissions().add(permission("BOOK_UPDATE"));

        assertThat(resolver.resolvePermissions(user))
                .containsExactly("BOOK_READ", "BOOK_UPDATE");
        assertThat(resolver.resolvePermissionSet(user))
                .isEqualTo(Set.of("BOOK_READ", "BOOK_UPDATE"));
    }

    @Test
    void shouldReturnEmptyPermissionsWhenUserHasNoGroups() {
        User user = new User();

        assertThat(resolver.resolveGroups(user)).isEmpty();
        assertThat(resolver.resolvePermissions(user)).isEmpty();
        assertThat(resolver.resolvePermissionSet(user)).isEmpty();
    }

    private User userWithGroups(String... names) {
        User user = new User();
        for (String name : names) {
            Group group = new Group();
            group.setName(name);
            user.getGroups().add(group);
        }
        return user;
    }

    private Permission permission(String name) {
        Permission permission = new Permission();
        permission.setName(name);
        return permission;
    }
}
