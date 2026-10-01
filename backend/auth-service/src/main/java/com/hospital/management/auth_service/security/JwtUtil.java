package com.hospital.management.auth_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKeyString;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserDetails userDetails, Long userId, Long serviceId) {
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("role", role)
                .claim("userId", userId)
                .claim("serviceId", serviceId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }
}