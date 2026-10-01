package com.hospital.management.doctorprofile.repository;


import com.hospital.management.doctorprofile.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {



    List<Doctor> findBySpecializationIgnoreCase(String specialization);


    boolean existsByEmail(String email);
}
