package com.hospital.management.doctorprofile.exception;

import org.springframework.http.HttpStatus;

public class DoctorAlreadyExistsException extends RuntimeException {

    private final HttpStatus status;

    public DoctorAlreadyExistsException(String message) {
        super(message);
        this.status = HttpStatus.CONFLICT;
    }

    public HttpStatus getStatus() {
        return status;
    }
}