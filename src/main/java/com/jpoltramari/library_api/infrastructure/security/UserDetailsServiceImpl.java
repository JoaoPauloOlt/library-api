package com.jpoltramari.library_api.infrastructure.security;

import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.infrastructure.security.rbac.RbacResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository repository;
    private final RbacResolver rbacResolver;

    @Override
    public UserDetails loadUserByUsername(String email) {
        var user = repository.findByEmailWithGroupsAndPermissions(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return AuthenticatedUser.fromUser(user, rbacResolver);
    }
}
