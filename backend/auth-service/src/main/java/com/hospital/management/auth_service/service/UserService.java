package com.hospital.management.auth_service.service;

import com.hospital.management.auth_service.client.DoctorServiceClient;
import com.hospital.management.auth_service.client.PatientServiceClient;
import com.hospital.management.auth_service.dto.*;
import com.hospital.management.auth_service.enums.Role;
import com.hospital.management.auth_service.exception.EmailException;
import com.hospital.management.auth_service.exception.ProfileCreationException;
import com.hospital.management.auth_service.exception.ResourceNotFoundException;
import com.hospital.management.auth_service.modal.PendingDoctorEntity;
import com.hospital.management.auth_service.modal.UserEntity;
import com.hospital.management.auth_service.repository.PendingDoctorRepository;
import com.hospital.management.auth_service.repository.UserRepository;
import com.hospital.management.auth_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import com.hospital.management.auth_service.modal.OtpEntity;
import com.hospital.management.auth_service.repository.OtpRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientServiceClient patientServiceClient;
    private final DoctorServiceClient doctorServiceClient;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PendingDoctorRepository pendingDoctorRepository;
    private final OtpRepository otpRepository;
    private final JavaMailSender mailSender;


    @Transactional
    public void registerUser(UserDTO userDTO) {
        validateUserDTO(userDTO);

        // Fail fast with a clean message before hitting a DB constraint
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new EmailException(
                    "Email already registered: " + userDTO.getEmail());
        }

        Role role = parseRole(userDTO.getRole());

        UserEntity user = UserEntity.builder()
                .email(userDTO.getEmail())
                .password(passwordEncoder.encode(userDTO.getPassword()))
                .role(String.valueOf(role))
                .status(role == Role.DOCTOR ? "PENDING" : "ACTIVE")
                .build();


        userRepository.save(user);
        log.info("User saved in authDB — userId={}, role={}", user.getUserId(), role);

        createDownstreamProfile(userDTO, role, user.getUserId(), user);
    }

    public LoginResponseDTO loginUser(LoginDTO loginDTO) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getEmail(),
                            loginDTO.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        UserEntity user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in DB: " + loginDTO.getEmail()));

        if ("PENDING".equals(user.getStatus())) {                          // ← ADD
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,        // ← ADD
                    "Your account is pending admin approval.");                 // ← ADD
        }                                                                   // ← ADD

        String token = jwtUtil.generateToken(userDetails, user.getUserId(), user.getServiceId());

        log.info("Login successful — userId={}, role={}", user.getUserId(), user.getRole());

        return LoginResponseDTO.builder()
                .token(token)
                .userId(user.getUserId())
                .serviceId(user.getServiceId())
                .role(user.getRole())
                .build();
    }

    // Private helpers

    private void validateUserDTO(UserDTO userDTO) {
        if (!StringUtils.hasText(userDTO.getEmail())) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (!StringUtils.hasText(userDTO.getPassword())) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (!StringUtils.hasText(userDTO.getRole())) {
            throw new IllegalArgumentException("Role must not be blank");
        }
    }

    private Role parseRole(String rawRole) {
        try {
            return Role.valueOf(rawRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unsupported role: '" + rawRole + "'. Valid values: "
                            + java.util.Arrays.toString(Role.values()));
        }
    }

    private void createDownstreamProfile(UserDTO userDTO, Role role, Long userId, UserEntity user) {
        switch (role) {
            case PATIENT -> {
                try {
                    PatientRequestDTO patientRequest = PatientRequestDTO.builder()
                            .name(userDTO.getName())
                            .gender(userDTO.getGender())
                            .dateOfBirth(userDTO.getDateOfBirth())
                            .contactDetails(userDTO.getContactDetails())
                            .build();

                    PatientResponseDTO response = patientServiceClient.registerPatient(patientRequest);
                    Long patientId = response.getPatientId();

                    user.setServiceId(patientId);
                    userRepository.save(user);

                    log.info("Patient profile created — userId={}, patientId={}", userId, patientId);
                } catch (Exception e) {
                    log.error("Patient profile creation failed — userId={}, error={}",
                            userId, e.getMessage());
                    throw new ProfileCreationException(
                            "Could not create patient profile. Registration rolled back.", e);
                }
            }

            case DOCTOR -> {
                PendingDoctorEntity pending = PendingDoctorEntity.builder()
                        .userId(userId)
                        .name(userDTO.getName())
                        .email(userDTO.getEmail())
                        .contactDetails(userDTO.getContactDetails())
                        .specialization(userDTO.getSpecialization())
                        .designation(userDTO.getDesignation())
                        .build();
                pendingDoctorRepository.save(pending);  // ← store for later
                log.info("Doctor registration pending approval — userId={}", userId);
            }

            case ADMIN -> throw new IllegalArgumentException(
                    "Admin accounts cannot be self-registered.");
        }
    }
    public List<UserResponseDTO> getUsersByRole(String role) {
        Role parsedRole = parseRole(role);
        return userRepository.findByRole(parsedRole.name())
                .stream()
                .map(user -> UserResponseDTO.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .toList();
    }

    public String getEmailByServiceId(Long serviceId, String role) {
        return userRepository.findFirstByServiceIdAndRole(serviceId, role)
                .map(UserEntity::getEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with serviceId: " + serviceId + " and role: " + role));
    }


    public String getEmailByUserId(Long userId) {
        return userRepository.findByUserId(userId)
                .map(UserEntity::getEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with userId: " + userId));
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> UserResponseDTO.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .toList();
    }


    @Transactional
    public void approveDoctorRegistration(Long userId) {
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (!"PENDING".equals(user.getStatus())) {
            throw new IllegalStateException("User is not in PENDING state.");
        }

        // Fetch stored doctor details and create profile now
        PendingDoctorEntity pending = pendingDoctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pending doctor data not found: " + userId));

        DoctorRequestDTO doctorRequest = DoctorRequestDTO.builder()
                .name(pending.getName())
                .email(pending.getEmail())
                .contactDetails(pending.getContactDetails())
                .specialization(pending.getSpecialization())
                .designation(pending.getDesignation())
                .build();

        DoctorResponseDTO response = doctorServiceClient.registerDoctor(doctorRequest);
        user.setServiceId(response.getId());
        user.setStatus("ACTIVE");
        userRepository.save(user);
        pendingDoctorRepository.delete(pending);  // clean up temp data
        log.info("Doctor approved and profile created — userId={}, doctorId={}", userId, response.getId());
    }

    @Transactional
    public void rejectDoctorRegistration(Long userId) {
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (!"PENDING".equals(user.getStatus())) {
            throw new IllegalStateException("User is not in PENDING state.");
        }
        pendingDoctorRepository.findByUserId(userId)
                .ifPresent(pendingDoctorRepository::delete);
        userRepository.delete(user);
        log.info("Doctor rejected and removed — userId={}", userId);
    }

    public List<UserResponseDTO> getPendingDoctors() {
        return userRepository.findByRoleAndStatus("DOCTOR", "PENDING")
                .stream()
                .map(user -> UserResponseDTO.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .toList();
    }
    @Transactional
    public void deleteUserByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Delete downstream profile based on role
        if ("DOCTOR".equals(user.getRole()) && user.getServiceId() != null) {
            try {
                doctorServiceClient.deleteDoctor(user.getServiceId());
                log.info("Doctor profile deleted for userId={}", user.getUserId());
            } catch (Exception e) {
                log.warn("Failed to delete doctor profile for userId={}: {}", user.getUserId(), e.getMessage());
            }
        } else if ("PATIENT".equals(user.getRole()) && user.getServiceId() != null) {
            try {
                patientServiceClient.deletePatient(user.getServiceId());
                log.info("Patient profile deleted for userId={}", user.getUserId());
            } catch (Exception e) {
                log.warn("Failed to delete patient profile for userId={}: {}", user.getUserId(), e.getMessage());
            }
        }

        userRepository.delete(user);
        log.info("Auth user deleted email={}", email);
    }

    @Transactional
    public void sendOtp(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + email));

        otpRepository.deleteByEmail(email);

        String otp = String.format("%06d", new Random().nextInt(999999));

        OtpEntity otpEntity = OtpEntity.builder()
                .email(email)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
        otpRepository.save(otpEntity);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("HAMS - Password Reset OTP");
        message.setText("Your OTP for password reset is: " + otp + "\n\nThis OTP expires in 5 minutes.\n\nIf you did not request this, ignore this email.");
        mailSender.send(message);

        log.info("OTP sent to {}", email);
    }

    @Transactional
    public void verifyOtpAndResetPassword(String email, String otp, String newPassword) {
        OtpEntity otpEntity = otpRepository.findByEmailAndUsed(email, false)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP."));

        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        if (!otpEntity.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Incorrect OTP. Please try again.");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        otpEntity.setUsed(true);
        otpRepository.save(otpEntity);

        log.info("Password reset via OTP for {}", email);
    }
    public void verifyOtp(String email, String otp) {
        OtpEntity otpEntity = otpRepository.findByEmailAndUsed(email, false)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP."));

        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        if (!otpEntity.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Incorrect OTP. Please try again.");
        }
    }
}