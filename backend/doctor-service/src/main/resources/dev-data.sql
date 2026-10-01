-- ============================================================
-- doctor-service dev sample data
-- Re-run safe: INSERT IGNORE skips rows that already exist.
-- Dates use DATE_ADD(CURDATE(), INTERVAL 5 DAY) so sample data is
-- always 5 days in the future regardless of when the script runs.
-- Trade-off: slots are always "upcoming"; no historical slot sample data.
-- ============================================================

-- doctor_availability before doctor_slot (FK dependency within same service)
INSERT IGNORE INTO doctor_availability
    (id, doctor_id, date, shift_start, shift_end, break_start, is_available, is_locked)
VALUES
  (1, 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '09:00:00', '17:00:00', '13:00:00', true, false),
  (2, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '10:00:00', '16:00:00', '13:00:00', true, false);

INSERT IGNORE INTO doctor_slot
    (slot_id, doctor_id, slot_date, start_time, is_booked, patient_id, version, created_at)
VALUES
  (1,  1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '09:00:00', true,  1,    1, NOW()),
  (2,  1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '09:30:00', false, NULL, 0, NOW()),
  (3,  1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '10:00:00', false, NULL, 0, NOW()),
  (4,  1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '10:30:00', false, NULL, 0, NOW()),
  (5,  1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '11:00:00', false, NULL, 0, NOW()),
  (6,  1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '11:30:00', false, NULL, 0, NOW()),
  (7,  1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '12:00:00', false, NULL, 0, NOW()),
  (8,  1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '12:30:00', false, NULL, 0, NOW()),

  (9,  1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '14:00:00', false, NULL, 0, NOW()),
  (10, 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '14:30:00', false, NULL, 0, NOW()),
  (11, 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '15:00:00', false, NULL, 0, NOW()),
  (12, 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '15:30:00', false, NULL, 0, NOW()),
  (13, 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '16:00:00', false, NULL, 0, NOW()),
  (14, 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '16:30:00', false, NULL, 0, NOW());


INSERT IGNORE INTO doctor_slot
    (slot_id, doctor_id, slot_date, start_time, is_booked, patient_id, version, created_at)
VALUES
  (15, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '10:00:00', false, NULL, 0, NOW()),
  (16, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '10:30:00', false, NULL, 0, NOW()),
  (17, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '11:00:00', false, NULL, 0, NOW()),
  (18, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '11:30:00', false, NULL, 0, NOW()),
  (19, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '12:00:00', false, NULL, 0, NOW()),
  (20, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '12:30:00', false, NULL, 0, NOW()),

  (21, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '13:30:00', false, NULL, 0, NOW()),
  (22, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '14:00:00', false, NULL, 0, NOW()),
  (23, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '14:30:00', false, NULL, 0, NOW()),
  (24, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '15:00:00', false, NULL, 0, NOW()),
  (25, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '15:30:00', false, NULL, 0, NOW());
