-- ============================================================
-- patient-service dev sample data
-- Re-run safe: INSERT IGNORE skips rows that already exist.
-- patientId must match auth-service.users.service_id for PATIENT rows.
-- ============================================================

INSERT IGNORE INTO patient_profiles (patient_id, name, date_of_birth, gender, contact_details)
VALUES
  (1, 'SanthoshKumar',  '1990-05-15', 'MALE',   '9876543210'),
  (2, 'Priya Kapoor',  '1995-08-22', 'FEMALE', '9876543211'),
  (3, 'Rahul Verma',   '1985-03-10', 'MALE',   '9876543212');
