package com.hospital.management.apigateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "jwt.secret=7CB916ua56wKRz0UJuURnd2wSEg2C0S3",
        "eureka.client.enabled=false",
        "spring.cloud.gateway.discovery.locator.enabled=false"
})
@DisplayName("Gateway Security Layer Tests")
class GatewaySecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    private static final String SECRET = "7CB916ua56wKRz0UJuURnd2wSEg2C0S3";

    private String buildToken(String email, String role, Long userId, Long serviceId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId)
                .claim("serviceId", serviceId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }



    @Test
    @DisplayName("GET /medical-history-service/api/v1/5 with ROLE_PATIENT passes security")
    void medicalHistoryById_PatientToken_PassesSecurity() {
        String token = buildToken("patient@test.com", "ROLE_PATIENT", 2L, 5L);

        webTestClient.get()
                .uri("/medical-history-service/api/v1/5")
                .header("Authorization", "Bearer " + token)
                .exchange()

                .expectStatus().value(status ->
                        org.junit.jupiter.api.Assertions.assertNotEquals(401, status,
                                "Expected security to pass but got 401 Unauthorized"));
    }

    @Test
    @DisplayName("GET /medical-history-service/api/v1/all with ROLE_PATIENT is blocked at gateway (403)")
    void medicalHistoryAll_PatientToken_Forbidden() {
        String token = buildToken("patient@test.com", "ROLE_PATIENT", 2L, 5L);

        webTestClient.get()
                .uri("/medical-history-service/api/v1/all")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("GET /medical-history-service/api/v1/5 with no token is rejected (401)")
    void medicalHistoryById_NoToken_Unauthorized() {
        webTestClient.get()
                .uri("/medical-history-service/api/v1/5")
                .exchange()
                .expectStatus().isUnauthorized();
    }



    @Test
    @DisplayName("PUT /doctor-service/api/v1/doctor-schedule/slots/claim with any token is denied (403)")
    void slotsCllaim_AnyRole_DenyAll() {
        String token = buildToken("patient@test.com", "ROLE_PATIENT", 2L, 1L);

        webTestClient.put()
                .uri("/doctor-service/api/v1/doctor-schedule/slots/claim?doctorId=1&patientId=1&startTime=2030-12-15T10:00:00")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }



    @Test
    @DisplayName("PATCH /appointment-service/api/v1/appointments/cancel/{code} with ROLE_PATIENT passes security")
    void cancelAppointment_PatchMethod_PatientToken_PassesSecurity() {
        String token = buildToken("patient@test.com", "ROLE_PATIENT", 2L, 1L);

        webTestClient.patch()
                .uri("/appointment-service/api/v1/appointments/cancel/some-code")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().value(status ->
                        org.junit.jupiter.api.Assertions.assertNotEquals(403, status,
                                "Expected ROLE_PATIENT to be allowed to PATCH cancel"));
    }

    @Test
    @DisplayName("PATCH /appointment-service/api/v1/appointments/complete/{code} with ROLE_DOCTOR passes security")
    void completeAppointment_PatchMethod_DoctorToken_PassesSecurity() {
        String token = buildToken("doctor@test.com", "ROLE_DOCTOR", 5L, 1L);

        webTestClient.patch()
                .uri("/appointment-service/api/v1/appointments/complete/some-code")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().value(status ->
                        org.junit.jupiter.api.Assertions.assertNotEquals(403, status,
                                "Expected ROLE_DOCTOR to be allowed to PATCH complete"));
    }

    @Test
    @DisplayName("PATCH /appointment-service/api/v1/appointments/complete/{code} with ROLE_PATIENT is blocked (403)")
    void completeAppointment_PatientToken_Forbidden() {
        String token = buildToken("patient@test.com", "ROLE_PATIENT", 2L, 1L);

        webTestClient.patch()
                .uri("/appointment-service/api/v1/appointments/complete/some-code")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }
}
