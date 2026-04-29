package com.jpoltramari.library_api.infrastructure.security;

import com.jpoltramari.library_api.domain.model.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

public class UserDetailsImpl implements UserDetails {

    private final User user;

    public UserDetailsImpl(User user){
        this.user = user;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public java.util.Collection getAuthorities() {
        return Collections.emptyList();
    }
}