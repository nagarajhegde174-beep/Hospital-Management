-- ============================================================
-- medical-history-service dev sample data
-- Re-run safe: INSERT IGNORE skips rows that already exist.
-- patientId values must match patient-service.patient_profiles.patient_id.
-- doctorId values must match doctor-profile-service.doctors.id.
-- ============================================================

-- Backfill doctor_id on existing rows that were seeded before the column was added
UPDATE medical_history SET doctor_id = 1 WHERE record_id = 1 AND doctor_id IS NULL;
UPDATE medical_history SET doctor_id = 2 WHERE record_id = 2 AND doctor_id IS NULL;
UPDATE medical_history SET doctor_id = 1 WHERE record_id = 3 AND doctor_id IS NULL;
-- Catch-all: assign any remaining null doctor_ids to doctor 1
UPDATE medical_history SET doctor_id = 1 WHERE doctor_id IS NULL;

INSERT IGNORE INTO medical_history
    (record_id, diagnosis, diagnosed_at, patient_id, doctor_id, created_at, updated_at)
VALUES
  (1, 'Hypertension Stage 1',       DATE_SUB(CURDATE(), INTERVAL 90 DAY),  1, 1, NOW(), NOW()),
  (2, 'Pediatric routine checkup',  DATE_SUB(CURDATE(), INTERVAL 30 DAY),  2, 2, NOW(), NOW()),
  (3, 'Acute bronchitis',           DATE_SUB(CURDATE(), INTERVAL 45 DAY),  1, 1, NOW(), NOW());

INSERT IGNORE INTO prescribed_medications (record_id, prescribed_meds)
VALUES
  (1, 'Amlodipine 5mg daily'),
  (1, 'Telmisartan 40mg daily'),
  (3, 'Azithromycin 500mg 3 days'),
  (3, 'Salbutamol inhaler');
