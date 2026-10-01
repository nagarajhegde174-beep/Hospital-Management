package com.hospital.appointmentservice.repository;

import com.hospital.appointmentservice.model.Appointment;
import com.hospital.appointmentservice.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Checks if a doctor is already busy at a specific time
    boolean existsByDoctorIdAndAppointmentDate(Long doctorId, LocalDateTime appointmentDate);

    boolean existsByPatientIdAndAppointmentDate(Long patientId, LocalDateTime appointmentDate);

    boolean existsByDoctorIdAndAppointmentDateAndStatusNot(
            Long doctorId,
            LocalDateTime appointmentDate,
            AppointmentStatus status);


    boolean existsByPatientIdAndDoctorIdAndAppointmentDate(Long patientId, Long doctorId, LocalDateTime appointmentDate);

    // Find appointment by confirmation code
    Optional<Appointment> findByConfirmationCode(String confirmationCode);

    boolean existsByPatientIdAndAppointmentDateAndStatusNot(
            Long patientId,
            LocalDateTime appointmentDate,
            AppointmentStatus status);

    boolean existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusNot(
            Long patientId,
            Long doctorId,
            LocalDateTime appointmentDate,
            AppointmentStatus status);

    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(Long patientId);

    List<Appointment> findByDoctorIdOrderByAppointmentDateDesc(Long doctorId);
}