package com.hospital.appointmentservice.client;

import com.hospital.appointmentservice.client.fallback.NotificationServiceClientFallback;
import com.hospital.appointmentservice.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service",fallback = NotificationServiceClientFallback.class)
public interface NotificationServiceClient {

    @PostMapping("/api/v1/notifications")
    void sendNotification(@RequestBody NotificationRequest request);
}
