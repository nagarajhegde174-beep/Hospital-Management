package com.hospital.management.auth_service.service;

import com.hospital.management.auth_service.client.DoctorServiceClient;
import com.hospital.management.auth_service.client.PatientServiceClient;
import com.hospital.management.auth_service.dto.*;
import com.hospital.management.auth_service.exception.EmailException;
import com.hospital.management.auth_service.exception.ProfileCreationException;
import com.hospital.management.auth_service.exception.ResourceNotFoundException;
import com.hospital.management.auth_service.modal.OtpEntity;
import com.hospital.management.auth_service.modal.PendingDoctorEntity;
import com.hospital.management.auth_service.modal.UserEntity;
import com.hospital.management.auth_service.repository.OtpRepository;
import com.hospital.management.auth_service.repository.PendingDoctorRepository;
import com.hospital.management.auth_service.repository.UserRepository;
import com.hospital.management.auth_service.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PatientServiceClient patientServiceClient;
    @Mock private DoctorServiceClient doctorServiceClient;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private PendingDoctorRepository pendingDoctorRepository;
    @Mock private OtpRepository otpRepository;
    @Mock private JavaMailSender mailSender;

    @InjectMocks
    private UserService userService;

    // ─── registerUser ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("registerUser: patient registration creates user and downstream profile")
    void registerUser_patient_success() {
        UserDTO dto = patientDto();
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity(1L, "PATIENT"));

        PatientResponseDTO patientResp = new PatientResponseDTO();
        patientResp.setPatientId(101L);
        when(patientServiceClient.registerPatient(any())).thenReturn(patientResp);

        assertDoesNotThrow(() -> userService.registerUser(dto));

        verify(userRepository, atLeastOnce()).save(any(UserEntity.class));
        verify(patientServiceClient).registerPatient(any());
        verifyNoInteractions(doctorServiceClient);
    }

    @Test
    @DisplayName("registerUser: doctor registration saves to pending queue, does NOT call doctorServiceClient")
    void registerUser_doctor_success() {
        UserDTO dto = doctorDto();
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity(2L, "DOCTOR"));

        assertDoesNotThrow(() -> userService.registerUser(dto));

        verify(userRepository, atLeastOnce()).save(any(UserEntity.class));
        verify(pendingDoctorRepository, atLeastOnce()).save(any(PendingDoctorEntity.class));
        verifyNoInteractions(doctorServiceClient);
        verifyNoInteractions(patientServiceClient);
    }

    @Test
    @DisplayName("registerUser: duplicate email throws EmailException")
    void registerUser_duplicateEmail_throwsEmailException() {
        UserDTO dto = patientDto();
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThrows(EmailException.class, () -> userService.registerUser(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUser: ADMIN role throws IllegalArgumentException")
    void registerUser_adminRole_throwsIllegalArgumentException() {
        UserDTO dto = patientDto();
        dto.setRole("ADMIN");
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(userEntity(1L, "ADMIN"));

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(dto));
    }

    @Test
    @DisplayName("registerUser: unknown role throws IllegalArgumentException")
    void registerUser_invalidRole_throwsIllegalArgumentException() {
        UserDTO dto = patientDto();
        dto.setRole("SUPERUSER");
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(dto));
    }

    @Test
    @DisplayName("registerUser: blank email throws IllegalArgumentException")
    void registerUser_blankEmail_throwsIllegalArgumentException() {
        UserDTO dto = patientDto();
        dto.setEmail("  ");

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(dto));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("registerUser: patient client failure throws ProfileCreationException")
    void registerUser_patientClientFailure_throwsProfileCreationException() {
        UserDTO dto = patientDto();
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(userEntity(1L, "PATIENT"));
        when(patientServiceClient.registerPatient(any()))
                .thenThrow(new RuntimeException("patient-service down"));

        assertThrows(ProfileCreationException.class, () -> userService.registerUser(dto));
    }

    // ─── loginUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("loginUser: valid credentials return LoginResponseDTO with token")
    void loginUser_validCredentials_returnsToken() {
        LoginDTO dto = new LoginDTO();
        dto.setEmail("patient@test.com");
        dto.setPassword("Password1");

        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);

        UserEntity user = userEntity(1L, "ROLE_PATIENT");
        user.setStatus("ACTIVE");
        user.setServiceId(101L);
        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any(), anyLong(), anyLong())).thenReturn("jwt-token");

        LoginResponseDTO result = userService.loginUser(dto);

        assertEquals("jwt-token", result.getToken());
        assertEquals("ROLE_PATIENT", result.getRole());
        assertEquals(1L, result.getUserId());
    }

    @Test
    @DisplayName("loginUser: bad credentials throw ResponseStatusException 401")
    void loginUser_badCredentials_throwsUnauthorized() {
        LoginDTO dto = new LoginDTO();
        dto.setEmail("wrong@test.com");
        dto.setPassword("wrongpass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> userService.loginUser(dto));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("loginUser: PENDING account throws ResponseStatusException 403")
    void loginUser_pendingAccount_throwsForbidden() {
        LoginDTO dto = new LoginDTO();
        dto.setEmail("doctor@test.com");
        dto.setPassword("Password1");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(mock(UserDetails.class));
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        UserEntity pendingUser = userEntity(2L, "DOCTOR");
        pendingUser.setStatus("PENDING");
        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(pendingUser));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> userService.loginUser(dto));
        assertEquals(403, ex.getStatusCode().value());
    }

    // ─── sendOtp ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendOtp: sends OTP email to existing user")
    void sendOtp_success() {
        when(userRepository.findByEmail("patient@test.com"))
                .thenReturn(Optional.of(userEntity(1L, "PATIENT")));
        doNothing().when(otpRepository).deleteByEmail("patient@test.com");
        when(otpRepository.save(any(OtpEntity.class))).thenReturn(otpEntity());
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> userService.sendOtp("patient@test.com"));

        verify(otpRepository).deleteByEmail("patient@test.com");
        verify(otpRepository).save(any(OtpEntity.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendOtp: unknown email throws ResourceNotFoundException")
    void sendOtp_notFound_throwsException() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.sendOtp("unknown@test.com"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ─── verifyOtpAndResetPassword ────────────────────────────────────────────

    @Test
    @DisplayName("verifyOtpAndResetPassword: valid OTP resets password")
    void verifyOtpAndResetPassword_success() {
        OtpEntity otp = otpEntity();
        when(otpRepository.findByEmailAndUsed("patient@test.com", false))
                .thenReturn(Optional.of(otp));
        when(userRepository.findByEmail("patient@test.com"))
                .thenReturn(Optional.of(userEntity(1L, "PATIENT")));
        when(passwordEncoder.encode("NewPass1")).thenReturn("encodedNew");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(otpRepository.save(any())).thenReturn(otp);

        assertDoesNotThrow(() -> userService.verifyOtpAndResetPassword(
                "patient@test.com", "123456", "NewPass1"));

        verify(passwordEncoder).encode("NewPass1");
        verify(userRepository).save(any(UserEntity.class));
        assertTrue(otp.isUsed());
    }

    @Test
    @DisplayName("verifyOtpAndResetPassword: no unused OTP throws IllegalArgumentException")
    void verifyOtpAndResetPassword_noOtp_throwsException() {
        when(otpRepository.findByEmailAndUsed("patient@test.com", false))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> userService.verifyOtpAndResetPassword(
                        "patient@test.com", "123456", "NewPass1"));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("verifyOtpAndResetPassword: expired OTP throws IllegalArgumentException")
    void verifyOtpAndResetPassword_expiredOtp_throwsException() {
        OtpEntity expiredOtp = OtpEntity.builder()
                .email("patient@test.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .used(false)
                .build();
        when(otpRepository.findByEmailAndUsed("patient@test.com", false))
                .thenReturn(Optional.of(expiredOtp));

        assertThrows(IllegalArgumentException.class,
                () -> userService.verifyOtpAndResetPassword(
                        "patient@test.com", "123456", "NewPass1"));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("verifyOtpAndResetPassword: wrong OTP throws IllegalArgumentException")
    void verifyOtpAndResetPassword_wrongOtp_throwsException() {
        OtpEntity otp = otpEntity();
        when(otpRepository.findByEmailAndUsed("patient@test.com", false))
                .thenReturn(Optional.of(otp));

        assertThrows(IllegalArgumentException.class,
                () -> userService.verifyOtpAndResetPassword(
                        "patient@test.com", "999999", "NewPass1"));
        verify(passwordEncoder, never()).encode(any());
    }

    // ─── approveDoctorRegistration ────────────────────────────────────────────

    @Test
    @DisplayName("approveDoctorRegistration: approves pending doctor and creates profile")
    void approveDoctorRegistration_success() {
        UserEntity pendingUser = userEntity(2L, "DOCTOR");
        pendingUser.setStatus("PENDING");
        when(userRepository.findByUserId(2L)).thenReturn(Optional.of(pendingUser));

        PendingDoctorEntity pending = PendingDoctorEntity.builder()
                .userId(2L).name("Dr. Test").email("doctor@test.com")
                .contactDetails("9876543211").specialization("Cardiology")
                .designation("Consultant").build();
        when(pendingDoctorRepository.findByUserId(2L)).thenReturn(Optional.of(pending));

        DoctorResponseDTO doctorResp = new DoctorResponseDTO();
        doctorResp.setId(101L);
        when(doctorServiceClient.registerDoctor(any())).thenReturn(doctorResp);
        when(userRepository.save(any())).thenReturn(pendingUser);

        assertDoesNotThrow(() -> userService.approveDoctorRegistration(2L));

        verify(userRepository).save(any(UserEntity.class));
        verify(pendingDoctorRepository).delete(pending);
        assertEquals("ACTIVE", pendingUser.getStatus());
        assertEquals(101L, pendingUser.getServiceId());
    }

    @Test
    @DisplayName("approveDoctorRegistration: user not found throws ResourceNotFoundException")
    void approveDoctorRegistration_userNotFound_throwsException() {
        when(userRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.approveDoctorRegistration(99L));
    }

    @Test
    @DisplayName("approveDoctorRegistration: non-pending user throws IllegalStateException")
    void approveDoctorRegistration_notPending_throwsIllegalStateException() {
        UserEntity activeUser = userEntity(2L, "DOCTOR");
        activeUser.setStatus("ACTIVE");
        when(userRepository.findByUserId(2L)).thenReturn(Optional.of(activeUser));

        assertThrows(IllegalStateException.class,
                () -> userService.approveDoctorRegistration(2L));
        verifyNoInteractions(pendingDoctorRepository);
    }

    // ─── rejectDoctorRegistration ─────────────────────────────────────────────

    @Test
    @DisplayName("rejectDoctorRegistration: rejects pending doctor and deletes records")
    void rejectDoctorRegistration_success() {
        UserEntity pendingUser = userEntity(2L, "DOCTOR");
        pendingUser.setStatus("PENDING");
        when(userRepository.findByUserId(2L)).thenReturn(Optional.of(pendingUser));

        PendingDoctorEntity pending = PendingDoctorEntity.builder()
                .userId(2L).email("doctor@test.com").build();
        when(pendingDoctorRepository.findByUserId(2L)).thenReturn(Optional.of(pending));

        assertDoesNotThrow(() -> userService.rejectDoctorRegistration(2L));

        verify(pendingDoctorRepository).delete(pending);
        verify(userRepository).delete(pendingUser);
    }

    @Test
    @DisplayName("rejectDoctorRegistration: user not found throws ResourceNotFoundException")
    void rejectDoctorRegistration_userNotFound_throwsException() {
        when(userRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.rejectDoctorRegistration(99L));
    }

    @Test
    @DisplayName("rejectDoctorRegistration: non-pending user throws IllegalStateException")
    void rejectDoctorRegistration_notPending_throwsIllegalStateException() {
        UserEntity activeUser = userEntity(2L, "DOCTOR");
        activeUser.setStatus("ACTIVE");
        when(userRepository.findByUserId(2L)).thenReturn(Optional.of(activeUser));

        assertThrows(IllegalStateException.class,
                () -> userService.rejectDoctorRegistration(2L));
        verify(userRepository, never()).delete(any(UserEntity.class));
    }

    // ─── deleteUserByEmail ────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteUserByEmail: deletes existing user")
    void deleteUserByEmail_success() {
        UserEntity user = userEntity(1L, "PATIENT");
        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> userService.deleteUserByEmail("patient@test.com"));
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("deleteUserByEmail: unknown email throws RuntimeException")
    void deleteUserByEmail_notFound_throwsException() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.deleteUserByEmail("unknown@test.com"));
        verify(userRepository, never()).delete(any());
    }

    // ─── getPendingDoctors ────────────────────────────────────────────────────

    @Test
    @DisplayName("getPendingDoctors: returns list of pending doctors")
    void getPendingDoctors_returnsList() {
        UserEntity pending = userEntity(2L, "DOCTOR");
        pending.setStatus("PENDING");
        when(userRepository.findByRoleAndStatus("DOCTOR", "PENDING"))
                .thenReturn(List.of(pending));

        List<UserResponseDTO> result = userService.getPendingDoctors();

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getUserId());
    }

    @Test
    @DisplayName("getPendingDoctors: returns empty list when none pending")
    void getPendingDoctors_empty() {
        when(userRepository.findByRoleAndStatus("DOCTOR", "PENDING"))
                .thenReturn(List.of());

        List<UserResponseDTO> result = userService.getPendingDoctors();

        assertTrue(result.isEmpty());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private UserDTO patientDto() {
        UserDTO dto = new UserDTO();
        dto.setEmail("patient@test.com");
        dto.setPassword("Password1");
        dto.setRole("PATIENT");
        dto.setName("Test Patient");
        dto.setGender("MALE");
        dto.setContactDetails("9876543210");
        return dto;
    }

    private UserDTO doctorDto() {
        UserDTO dto = new UserDTO();
        dto.setEmail("doctor@test.com");
        dto.setPassword("Password1");
        dto.setRole("DOCTOR");
        dto.setName("Dr. Test");
        dto.setSpecialization("Cardiology");
        dto.setDesignation("Consultant");
        dto.setContactDetails("9876543211");
        return dto;
    }

    private UserEntity userEntity(Long userId, String role) {
        return UserEntity.builder()
                .userId(userId)
                .email("test@test.com")
                .password("encoded")
                .role(role)
                .status("ACTIVE")
                .build();
    }

    private OtpEntity otpEntity() {
        return OtpEntity.builder()
                .email("patient@test.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
    }
}