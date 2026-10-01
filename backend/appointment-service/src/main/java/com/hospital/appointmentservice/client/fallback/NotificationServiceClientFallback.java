package com.hospital.appointmentservice.client.fallback;

import com.hospital.appointmentservice.client.NotificationServiceClient;
import com.hospital.appointmentservice.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class NotificationServiceClientFallback implements NotificationServiceClient {

    private static final Logger logger =
            LoggerFactory.getLogger(NotificationServiceClientFallback.class);

    @Override
    public void sendNotification(NotificationRequest request) {
        logger.warn("FALLBACK: notification-service unavailable. " +
                        "Notification dropped for confirmationCode={}, status={}.",
                request.getConfirmationCode(), request.getAppointmentStatus());

    }
}