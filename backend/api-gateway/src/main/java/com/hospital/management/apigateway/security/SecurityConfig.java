package com.hospital.management.apigateway.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {
        httpSecurity
                .authorizeExchange(exchanges -> exchanges


                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public endpoints
                        .pathMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        .pathMatchers(HttpMethod.POST,
                                "/auth-service/api/v1/auth/register",
                                "/auth-service/api/v1/auth/login",
                                "/auth-service/api/v1/auth/send-otp",
                                "/auth-service/api/v1/auth/verify-otp",
                                "/auth-service/api/v1/auth/verify-otp-reset"
                        ).permitAll()

                        .pathMatchers(HttpMethod.POST,
                                "/patient-service/api/v1/patients"
                        ).permitAll()

                        // Admin-only
                        .pathMatchers(
                                "/auth-service/api/v1/auth/users"
                        ).hasAuthority("ROLE_ADMIN")

                        .pathMatchers(HttpMethod.DELETE,
                                "/patient-service/api/v1/patients/**",
                                "/doctor-profile-service/api/v1/doctors/**"
                        ).hasAuthority("ROLE_ADMIN")

                        .pathMatchers(HttpMethod.GET,
                                "/auth-service/api/v1/auth/patient/{patientId}/email",
                                "/auth-service/api/v1/auth/doctor/{doctorId}/email",
                                "/auth-service/api/v1/auth/user/{userId}/email",
                                "/auth-service/api/v1/auth/all/users"
                        ).hasAuthority("ROLE_ADMIN")

                        .pathMatchers(HttpMethod.DELETE,
                                "/medical-history-service/api/**"
                        ).hasAuthority("ROLE_ADMIN")


                        .pathMatchers(HttpMethod.GET,
                                "/medical-history-service/api/v1/all"
                        ).hasAuthority("ROLE_ADMIN")

                        .pathMatchers(HttpMethod.GET,
                                "/medical-history-service/api/v1/{recordId:[0-9]+}"
                        ).hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT")


                        .pathMatchers(HttpMethod.PUT,
                                "/medical-history-service/api/v1/{recordId:[0-9]+}"
                        ).hasAuthority("ROLE_DOCTOR")


                        .pathMatchers(HttpMethod.GET,
                                "/medical-history-service/api/v1/patient/{patientId:[0-9]+}",
                                "/medical-history-service/api/v1/patient/{patientId:[0-9]+}/paged"
                        ).hasAnyAuthority("ROLE_ADMIN", "ROLE_PATIENT", "ROLE_DOCTOR")


                        .pathMatchers(HttpMethod.POST,
                                "/medical-history-service/api/**"
                        ).hasAnyAuthority("ROLE_DOCTOR", "ROLE_ADMIN")

                        // Doctor + Admin
                        .pathMatchers(HttpMethod.POST,
                                "/doctor-service/api/v1/doctor-schedule/set-availability",
                                "/doctor-service/api/v1/doctor-schedule/add-prescription/**"
                        ).hasAnyAuthority("ROLE_DOCTOR", "ROLE_ADMIN")

                        .pathMatchers(HttpMethod.GET,
                                "/doctor-service/api/v1/doctor-schedule/view-history/**"
                        ).hasAnyAuthority("ROLE_DOCTOR", "ROLE_ADMIN")

                        .pathMatchers(HttpMethod.PUT,
                                "/doctor-service/api/v1/doctor-schedule/slots/claim",
                                "/doctor-service/api/v1/doctor-schedule/cancel-booking"
                        ).denyAll()

                        // Patient + Doctor + Admin
                        .pathMatchers(HttpMethod.GET,
                                "/doctor-service/api/v1/doctor-schedule/available-dates/**",
                                "/doctor-service/api/v1/doctor-schedule/slots/**"
                        ).hasAnyAuthority("ROLE_PATIENT", "ROLE_DOCTOR", "ROLE_ADMIN")

                        .pathMatchers(HttpMethod.POST,
                                "/doctor-profile-service/api/v1/ratings"
                        ).hasAuthority("ROLE_PATIENT")

                        .pathMatchers(HttpMethod.GET,
                                "/doctor-profile-service/api/v1/ratings/doctor/*/average",
                                "/doctor-profile-service/api/v1/ratings/appointment/*/rated",
                                "/doctor-profile-service/api/v1/ratings/appointment/*/rating"
                        ).hasAnyAuthority("ROLE_PATIENT", "ROLE_DOCTOR", "ROLE_ADMIN")

                        .pathMatchers(HttpMethod.GET,
                                "/patient-service/api/v1/patients/**"
                        ).hasAnyAuthority("ROLE_PATIENT", "ROLE_DOCTOR", "ROLE_ADMIN")


                        .pathMatchers(HttpMethod.PATCH,
                                "/patient-service/api/v1/patients/**"
                        ).hasAnyAuthority("ROLE_PATIENT", "ROLE_ADMIN")

                        .pathMatchers(HttpMethod.GET,
                                "/patient-service/api/v1/patients"
                        ).hasAnyAuthority("ROLE_DOCTOR", "ROLE_ADMIN")

                        .pathMatchers(HttpMethod.GET,
                                "/doctor-profile-service/api/v1/doctors",
                                "/doctor-profile-service/api/v1/doctors/**"
                        ).hasAnyAuthority("ROLE_PATIENT", "ROLE_DOCTOR", "ROLE_ADMIN")

                        .pathMatchers(HttpMethod.PATCH,
                                "/doctor-profile-service/api/v1/doctors/**"
                        ).hasAnyAuthority("ROLE_DOCTOR", "ROLE_ADMIN")

                        //  Appointment service
                        .pathMatchers(HttpMethod.POST,
                                "/appointment-service/api/v1/appointments/available-doctors",
                                "/appointment-service/api/v1/appointments/available-slots"
                        ).hasAnyAuthority("ROLE_PATIENT", "ROLE_DOCTOR", "ROLE_ADMIN")

                        .pathMatchers(HttpMethod.GET,
                                "/appointment-service/api/v1/appointments/**"
                        ).hasAnyAuthority("ROLE_PATIENT", "ROLE_DOCTOR", "ROLE_ADMIN")


                        .pathMatchers(HttpMethod.POST,
                                "/appointment-service/api/v1/appointments/booking"
                        ).hasAnyAuthority("ROLE_PATIENT", "ROLE_ADMIN")


                        .pathMatchers(HttpMethod.PATCH,
                                "/appointment-service/api/v1/appointments/cancel/**",
                                "/appointment-service/api/v1/appointments/reschedule"
                        ).hasAnyAuthority("ROLE_PATIENT", "ROLE_ADMIN")

                        .pathMatchers(HttpMethod.PATCH,
                                "/appointment-service/api/v1/appointments/complete/**"
                        ).hasAnyAuthority("ROLE_DOCTOR", "ROLE_ADMIN")

                        .anyExchange().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return httpSecurity.build();
    }
}
