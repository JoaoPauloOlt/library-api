package com.jpoltramari.library_api.application.port.security;

public interface SessionRevocationPort {

    void revokeAllSessions(Long userId);
}
