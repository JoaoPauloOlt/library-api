package com.jpoltramari.library_api.application.port.security;

public interface AccessTokenPort {

    long getExpiration();

    void blacklistAccessToken(String token);
}
