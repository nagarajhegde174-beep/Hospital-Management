package com.hospital.management.medicalhistoryservice.exception;

public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(String msg){
        super(msg);
    }
}
