package com.jpoltramari.library_api.infrastructure.security;

import com.jpoltramari.library_api.infrastructure.security.jwt.JwtClaims;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Authentication token for stateless JWT requests.
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedUser principal;
    private final JwtClaims claims;

    public JwtAuthenticationToken(AuthenticatedUser principal, JwtClaims claims) {
        super(principal.getAuthorities());
        this.principal = principal;
        this.claims = claims;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return claims;
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return principal;
    }

    public JwtClaims getClaims() {
        return claims;
    }
}
