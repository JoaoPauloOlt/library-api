package com.jpoltramari.library_api.infrastructure.security;

import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.domain.util.EmailNormalizer;
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
        String normalizedEmail = EmailNormalizer.normalize(email);
        var user = repository.findByEmailWithGroupsAndPermissions(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedEmail));
        return AuthenticatedUser.fromUser(user, rbacResolver);
    }
}
