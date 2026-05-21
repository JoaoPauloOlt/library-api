package com.jpoltramari.library_api.infrastructure.security.rbac;

import com.jpoltramari.library_api.domain.model.Group;
import com.jpoltramari.library_api.domain.model.Permission;
import com.jpoltramari.library_api.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Resolves RBAC data from persisted groups at login time.
 * Permissions are flattened and deduplicated before being embedded in the JWT.
 */
@Component
public class RbacResolver {

    public List<String> resolveGroups(User user) {
        return user.getGroups().stream()
                .map(Group::getName)
                .sorted()
                .toList();
    }

    public List<String> resolvePermissions(User user) {
        return user.getGroups().stream()
                .flatMap(group -> group.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(TreeSet::new),
                        set -> List.copyOf(set)
                ));
    }

    public Set<String> resolvePermissionSet(User user) {
        return new TreeSet<>(resolvePermissions(user));
    }
}
