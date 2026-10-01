-- ============================================================
-- doctor-profile-service dev sample data
-- Re-run safe: INSERT IGNORE skips rows that already exist.
-- doctor id must match auth-service.users.service_id for DOCTOR rows.
-- email must match auth-service.users.email for the same doctor.
-- ============================================================

INSERT IGNORE INTO doctors (id, name, email, specialization, designation, contact_details)
VALUES
  (1, 'Sandy',  'sandy321.2004@gmail.com',  'Cardiology',   'Senior Consultant', '9123456780'),
  (2, 'Sneha Patel', 'dr.sneha.patel@gmail.com', 'Pediatrics',   'Consultant',        '9123456781'),
  (3, 'Vijay Menon', 'dr.vijay.menon@gmail.com', 'Orthopedics',  'Senior Consultant', '9123456782');
