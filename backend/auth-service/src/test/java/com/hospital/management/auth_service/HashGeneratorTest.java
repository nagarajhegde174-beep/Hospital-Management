package com.hospital.management.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGeneratorTest {

    @Test
    void generateHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("Admin@123  → " + encoder.encode("Admin@123"));
        System.out.println("Doctor@123 → " + encoder.encode("Doctor@123"));
        System.out.println("Patient@123→ " + encoder.encode("Patient@123"));
    }
}