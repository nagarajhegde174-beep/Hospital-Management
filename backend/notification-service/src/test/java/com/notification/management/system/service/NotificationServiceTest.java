package com.notification.management.system.service;

import com.notification.management.system.client.AuthServiceClient;
import com.notification.management.system.client.DoctorServiceClient;
import com.notification.management.system.client.PatientServiceClient;
import com.notification.management.system.dto.NotificationRequestDTO;
import com.notification.management.system.dto.AppointmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private DoctorServiceClient doctorServiceClient;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@gmail.com");
        ReflectionTestUtils.setField(emailService, "mailPassword", "test-password");
    }

    //  Helper

    private NotificationRequestDTO buildRequest(AppointmentStatus status) {
        NotificationRequestDTO dto = new NotificationRequestDTO();
        dto.setPatientId(1L);
        dto.setDoctorId(2L);
        dto.setConfirmationCode("ABC123");
        dto.setAppointmentStatus(status);
        dto.setDate(LocalDate.of(2025, 6, 15));
        dto.setTime(LocalTime.of(10, 30));
        return dto;
    }

    //  sendAppointmentEmails

    @Test
    void sendAppointmentEmails_Confirmed_SendsTwoEmails() {
        when(authServiceClient.getPatientEmail(1L)).thenReturn("patient@gmail.com");
        when(authServiceClient.getDoctorEmail(2L)).thenReturn("doctor@gmail.com");

        emailService.sendAppointmentEmails(buildRequest(AppointmentStatus.CONFIRMED));

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendAppointmentEmails_Cancelled_SendsTwoEmails() {
        when(authServiceClient.getPatientEmail(1L)).thenReturn("patient@gmail.com");
        when(authServiceClient.getDoctorEmail(2L)).thenReturn("doctor@gmail.com");

        emailService.sendAppointmentEmails(buildRequest(AppointmentStatus.CANCELLED));

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendAppointmentEmails_Rescheduled_SendsTwoEmails() {
        when(authServiceClient.getPatientEmail(1L)).thenReturn("patient@gmail.com");
        when(authServiceClient.getDoctorEmail(2L)).thenReturn("doctor@gmail.com");

        emailService.sendAppointmentEmails(buildRequest(AppointmentStatus.RESCHEDULED));

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendAppointmentEmails_Completed_SendsTwoEmails() {
        when(authServiceClient.getPatientEmail(1L)).thenReturn("patient@gmail.com");
        when(authServiceClient.getDoctorEmail(2L)).thenReturn("doctor@gmail.com");

        emailService.sendAppointmentEmails(buildRequest(AppointmentStatus.COMPLETED));

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    // ─── sendAppointmentEmails —

    @Test
    void sendAppointmentEmails_PatientEmailFetchFails_SkipsPatientEmail() {
        when(authServiceClient.getPatientEmail(1L))
                .thenThrow(new RuntimeException("Auth service down"));
        when(authServiceClient.getDoctorEmail(2L)).thenReturn("doctor@gmail.com");

        emailService.sendAppointmentEmails(buildRequest(AppointmentStatus.CONFIRMED));

        // Only doctor email sent — patient skipped due to null email
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendAppointmentEmails_DoctorEmailFetchFails_SkipsDoctorEmail() {
        when(authServiceClient.getPatientEmail(1L)).thenReturn("patient@gmail.com");
        when(authServiceClient.getDoctorEmail(2L))
                .thenThrow(new RuntimeException("Auth service down"));

        emailService.sendAppointmentEmails(buildRequest(AppointmentStatus.CANCELLED));

        // Only patient email sent — doctor skipped due to null email
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendAppointmentEmails_BothEmailFetchesFail_NoEmailsSent() {
        when(authServiceClient.getPatientEmail(1L))
                .thenThrow(new RuntimeException("Auth service down"));
        when(authServiceClient.getDoctorEmail(2L))
                .thenThrow(new RuntimeException("Auth service down"));

        emailService.sendAppointmentEmails(buildRequest(AppointmentStatus.RESCHEDULED));

        // No emails sent at all
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    //  sendAppointmentEmails — Mail sender failure

    @Test
    void sendAppointmentEmails_MailSenderThrows_DoesNotPropagateException() {
        when(authServiceClient.getPatientEmail(1L)).thenReturn("patient@gmail.com");
        when(authServiceClient.getDoctorEmail(2L)).thenReturn("doctor@gmail.com");
        doThrow(new RuntimeException("SMTP error")).when(mailSender)
                .send(any(SimpleMailMessage.class));

        // Should not throw — failures are swallowed inside sendEmail()
        assertDoesNotThrow(() ->
                emailService.sendAppointmentEmails(buildRequest(AppointmentStatus.COMPLETED))
        );
    }
}