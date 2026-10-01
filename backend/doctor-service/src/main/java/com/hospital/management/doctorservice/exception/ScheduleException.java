package com.hospital.management.doctorservice.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class ScheduleException extends RuntimeException {

    private final HttpStatus status;

    public ScheduleException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}