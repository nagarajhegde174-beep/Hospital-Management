-- ============================================================
-- appointment-service dev sample data
-- Re-run safe: INSERT IGNORE skips rows that already exist.
-- appointment_id_seq row is seeded in V1__baseline.sql; no change here.
-- Cross-service note: doctor-service must be running and its dev-data
-- must be loaded before appointment-service dev-data for slot coherence
-- (slot 1 booked=true in doctor-service matches appointment 1000 here).
-- ============================================================

INSERT IGNORE INTO appointment
    (appointment_id, confirmation_code, patient_id, doctor_id,
     appointment_date, status, version)
VALUES
  -- Upcoming confirmed — patient 1, doctor 1, slot 1 (today+5 at 09:00)
  (1000, UUID(), 1, 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY) + INTERVAL '09:00' HOUR_MINUTE, 'CONFIRMED',  0),
  -- Past completed  — patient 2, doctor 2 (today-30)
  (1001, UUID(), 2, 2, DATE_SUB(CURDATE(), INTERVAL 30 DAY) + INTERVAL '10:00' HOUR_MINUTE, 'COMPLETED', 0),
  -- Cancelled       — patient 3, doctor 3 (today-15)
  (1002, UUID(), 3, 3, DATE_SUB(CURDATE(), INTERVAL 15 DAY) + INTERVAL '14:00' HOUR_MINUTE, 'CANCELLED', 0);

-- Advance the sequence past the seeded appointments so new IDs start at 1003+
UPDATE appointment_id_seq SET next_val = 1003 WHERE next_val < 1003;
