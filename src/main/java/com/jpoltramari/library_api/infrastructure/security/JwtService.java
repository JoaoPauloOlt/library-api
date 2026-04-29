package com.jpoltramari.library_api.infrastructure.security;

import com.jpoltramari.library_api.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private Key getKey(){
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(User user){
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getKey())
                .compact();
    }

    public String getUsername(String token){
        return getClaims(token).getSubject();
    }

    public boolean isValid(String token){
        return getClaims(token).getExpiration().after(new Date());
    }

    private Claims getClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}