package com.hospital.management.doctorservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ScheduleException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id, HttpStatus.NOT_FOUND);
    }
}