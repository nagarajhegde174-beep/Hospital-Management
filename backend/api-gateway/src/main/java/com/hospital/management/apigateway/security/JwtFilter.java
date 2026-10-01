package com.hospital.management.apigateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter implements WebFilter {

    // Headers that carry identity — clients must never be trusted to set these.
    private static final List<String> INTERNAL_HEADERS =
            List.of("X-User-Id", "X-Service-Id", "X-User-Role");

    private final JwtUtil jwtUtil;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {

        // Strip any client-supplied identity headers before processing.
        // Without this a caller could inject X-User-Role: ROLE_ADMIN and bypass auth.
        ServerWebExchange sanitized = exchange.mutate()
                .request(r -> r.headers(h -> INTERNAL_HEADERS.forEach(h::remove)))
                .build();

        String header = sanitized.getRequest().getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            return chain.filter(sanitized);
        }

        String token = header.substring(7);

        try {
            String email = jwtUtil.extractEmail(token);

            if (email == null) {
                log.warn("JWT token present but email claim is null — rejecting request");
                sanitized.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return sanitized.getResponse().setComplete();
            }

            String role      = jwtUtil.extractRole(token);
            Long   userId    = jwtUtil.extractUserId(token);
            Long   serviceId = jwtUtil.extractServiceId(token);

            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            email, null, Collections.singleton(authority));

            ServerWebExchange mutatedExchange = sanitized.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.set("X-User-Id",    userId    != null ? userId.toString()    : "");
                        headers.set("X-Service-Id", serviceId != null ? serviceId.toString() : "");
                        headers.set("X-User-Role",  role);
                    }))
                    .build();

            return chain.filter(mutatedExchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));

        } catch (Exception e) {
            log.warn("JWT validation failed: {} — rejecting request", e.getMessage());
            sanitized.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return sanitized.getResponse().setComplete();
        }
    }
}