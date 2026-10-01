package com.notification.management.system.service;

import com.notification.management.system.client.AuthServiceClient;
import com.notification.management.system.client.DoctorServiceClient;
import com.notification.management.system.client.PatientServiceClient;
import com.notification.management.system.dto.NotificationRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final AuthServiceClient authServiceClient;
    private final PatientServiceClient patientServiceClient;
    private final DoctorServiceClient doctorServiceClient;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public void sendAppointmentEmails(NotificationRequestDTO dto) {
        log.info("Starting email send for confirmationCode: {}", dto.getConfirmationCode());

        if (mailPassword == null || mailPassword.isBlank()) {
            log.warn("MAIL_PASSWORD not configured — skipping email notifications. " +
                     "Set the MAIL_PASSWORD env var to a Gmail App Password to enable emails.");
            return;
        }

        String patientEmail = fetchPatientEmail(dto.getPatientId());
        String doctorEmail  = fetchDoctorEmail(dto.getDoctorId());
        String patientName  = fetchPatientName(dto.getPatientId());
        String doctorName   = fetchDoctorName(dto.getDoctorId());

        log.info("Sending to patientEmail={} doctorEmail={}", patientEmail, doctorEmail);

        sendEmail(patientEmail, buildPatientSubject(dto), buildPatientBody(dto, patientName, doctorName));
        sendEmail(doctorEmail,  buildDoctorSubject(dto),  buildDoctorBody(dto, patientName, doctorName));
    }

    private void sendEmail(String to, String subject, String body) {
        if (to == null) {
            log.warn("Skipping email — recipient address is null");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.warn("Failed to send email to {}. Error: {}", to, e.getMessage());
        }
    }

    private String buildPatientSubject(NotificationRequestDTO dto) {
        return "Appointment " + dto.getAppointmentStatus() + " — Code: " + dto.getConfirmationCode();
    }

    private String buildPatientBody(NotificationRequestDTO dto, String patientName, String doctorName) {
        return switch (dto.getAppointmentStatus()) {
            case CONFIRMED -> """
                    Dear %s,

                    Your appointment has been confirmed.

                    Doctor            : %s
                    Date              : %s
                    Time              : %s
                    Confirmation Code : %s

                    Please arrive 10 minutes early.
                    """.formatted(patientName, doctorName, dto.getDate(), dto.getTime(), dto.getConfirmationCode());
            case CANCELLED -> """
                    Dear %s,

                    Your appointment has been cancelled.

                    Doctor            : %s
                    Date              : %s
                    Time              : %s
                    Confirmation Code : %s

                    Please book a new appointment if needed.
                    """.formatted(patientName, doctorName, dto.getDate(), dto.getTime(), dto.getConfirmationCode());
            case RESCHEDULED -> """
                    Dear %s,

                    Your appointment has been rescheduled.

                    Doctor            : %s
                    New Date          : %s
                    New Time          : %s
                    Confirmation Code : %s
                    """.formatted(patientName, doctorName, dto.getDate(), dto.getTime(), dto.getConfirmationCode());
            case COMPLETED -> """
                    Dear %s,

                    Your appointment with %s on %s at %s has been completed.
                    Thank you for your visit.

                    Confirmation Code : %s
                    """.formatted(patientName, doctorName, dto.getDate(), dto.getTime(), dto.getConfirmationCode());
        };
    }

    private String buildDoctorSubject(NotificationRequestDTO dto) {
        return "Patient Appointment " + dto.getAppointmentStatus() + " — Code: " + dto.getConfirmationCode();
    }

    private String buildDoctorBody(NotificationRequestDTO dto, String patientName, String doctorName) {
        return switch (dto.getAppointmentStatus()) {
            case CONFIRMED -> """
                    Dear %s,

                    A new appointment has been confirmed with patient %s.

                    Date              : %s
                    Time              : %s
                    Confirmation Code : %s
                    """.formatted(doctorName, patientName, dto.getDate(), dto.getTime(), dto.getConfirmationCode());
            case CANCELLED -> """
                    Dear %s,

                    The appointment with patient %s has been cancelled.

                    Date              : %s
                    Time              : %s
                    Confirmation Code : %s
                    """.formatted(doctorName, patientName, dto.getDate(), dto.getTime(), dto.getConfirmationCode());
            case RESCHEDULED -> """
                    Dear %s,

                    The appointment with patient %s has been rescheduled.

                    New Date          : %s
                    New Time          : %s
                    Confirmation Code : %s
                    """.formatted(doctorName, patientName, dto.getDate(), dto.getTime(), dto.getConfirmationCode());
            case COMPLETED -> """
                    Dear %s,

                    The appointment with patient %s on %s at %s has been marked as completed.

                    Confirmation Code : %s
                    """.formatted(doctorName, patientName, dto.getDate(), dto.getTime(), dto.getConfirmationCode());
        };
    }

    private String fetchPatientName(Long patientId) {
        try {
            return patientServiceClient.getPatientById(patientId).getName();
        } catch (Exception e) {
            log.warn("Could not fetch name for patientId: {}. Error: {}", patientId, e.getMessage());
            return "Patient";
        }
    }

    private String fetchDoctorName(Long doctorId) {
        try {
            return doctorServiceClient.getDoctorById(doctorId).getName();
        } catch (Exception e) {
            log.warn("Could not fetch name for doctorId: {}. Error: {}", doctorId, e.getMessage());
            return "Doctor";
        }
    }

    private String fetchPatientEmail(Long patientId) {
        try {
            return authServiceClient.getPatientEmail(patientId);
        } catch (Exception e) {
            log.warn("Could not fetch email for patientId: {}. Error: {}", patientId, e.getMessage());
            return null;
        }
    }

    private String fetchDoctorEmail(Long doctorId) {
        try {
            return authServiceClient.getDoctorEmail(doctorId);
        } catch (Exception e) {
            log.warn("Could not fetch email for doctorId: {}. Error: {}", doctorId, e.getMessage());
            return null;
        }
    }
}