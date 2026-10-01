package com.hospital.management.auth_service.controller;

import com.hospital.management.auth_service.dto.LoginDTO;
import com.hospital.management.auth_service.dto.LoginResponseDTO;
import com.hospital.management.auth_service.dto.UserDTO;
import com.hospital.management.auth_service.dto.UserResponseDTO;
import com.hospital.management.auth_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserDTO userDTO) {
        userService.registerUser(userDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("User registered successfully");
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@Valid @RequestBody LoginDTO loginDTO) {
        LoginResponseDTO token = userService.loginUser(loginDTO);
        return ResponseEntity.ok(token);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDTO>> getUsersByRole(
            @RequestParam String role) {
        List<UserResponseDTO> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/all/users")
    public ResponseEntity<List<UserResponseDTO>> getUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/patient/{patientId}/email")
    public ResponseEntity<String> getPatientEmail(@PathVariable Long patientId) {
        return ResponseEntity.ok(userService.getEmailByServiceId(patientId, "PATIENT"));
    }

    @GetMapping("/doctor/{doctorId}/email")
    public ResponseEntity<String> getDoctorEmail(@PathVariable Long doctorId) {
        return ResponseEntity.ok(userService.getEmailByServiceId(doctorId, "DOCTOR"));
    }

    @GetMapping("/user/{userId}/email")
    public ResponseEntity<String> getUserEmail(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getEmailByUserId(userId));
    }

    @PatchMapping("/users/{userId}/approve")
    public ResponseEntity<String> approveDoctor(@PathVariable Long userId) {
        userService.approveDoctorRegistration(userId);
        return ResponseEntity.ok("Doctor approved successfully.");
    }

    @PatchMapping("/users/{userId}/reject")
    public ResponseEntity<String> rejectDoctor(@PathVariable Long userId) {
        userService.rejectDoctorRegistration(userId);
        return ResponseEntity.ok("Doctor rejected and removed.");
    }
    @GetMapping("/users/pending-doctors")
    public ResponseEntity<List<UserResponseDTO>> getPendingDoctors() {
        return ResponseEntity.ok(userService.getPendingDoctors());
    }


    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestParam String email) {
        userService.sendOtp(email);
        return ResponseEntity.ok("OTP sent to your email.");
    }

    @PostMapping("/verify-otp-reset")
    public ResponseEntity<String> verifyOtpAndReset(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword) {
        userService.verifyOtpAndResetPassword(email, otp, newPassword);
        return ResponseEntity.ok("Password reset successful.");
    }

    @DeleteMapping("/users/{email}")
    public ResponseEntity<String> deleteUserByEmail(@PathVariable String email) {
        userService.deleteUserByEmail(email);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(
            @RequestParam String email,
            @RequestParam String otp) {
        userService.verifyOtp(email, otp);
        return ResponseEntity.ok("OTP verified.");
    }

}