package com.notification.management.system.controller;

import com.notification.management.system.dto.NotificationRequestDTO;
import com.notification.management.system.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
public class NotificationController {

    private final EmailService emailService;
    private final Executor notificationExecutor;

    public NotificationController(EmailService emailService,
                                   @Qualifier("notificationExecutor") Executor notificationExecutor) {
        this.emailService = emailService;
        this.notificationExecutor = notificationExecutor;
    }

    @PostMapping
    public ResponseEntity<String> sendNotification(
            @RequestBody NotificationRequestDTO dto) {
        log.info("Received notification request for confirmationCode: {}",
                dto.getConfirmationCode());
        notificationExecutor.execute(() -> {
            try {
                emailService.sendAppointmentEmails(dto);
            } catch (Exception e) {
                log.error("Email sending failed: {}", e.getMessage(), e);
            }
        });
        return ResponseEntity.ok("Notification triggered successfully");
    }
}