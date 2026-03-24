-- Normalize legacy FIR data so current enums and DTO mappings do not crash at runtime.

UPDATE firs
SET status = 'PENDING_REVIEW'
WHERE status = 'PENDING';

UPDATE firs
SET status = 'SUBMITTED'
WHERE status IS NULL
   OR TRIM(status) = ''
   OR status NOT IN (
       'SUBMITTED',
       'PENDING_REVIEW',
       'NEEDS_INFO',
       'ACCEPTED',
       'ASSIGNED',
       'INVESTIGATION',
       'CLOSED',
       'REJECTED'
   );

UPDATE firs
SET category = 'OTHER'
WHERE category IS NULL
   OR category = '';

UPDATE firs f
LEFT JOIN police_stations ps ON ps.id = f.police_station_id
SET f.police_station_id = NULL
WHERE f.police_station_id IS NOT NULL
  AND ps.id IS NULL;

DELETE fa
FROM fir_assignments fa
JOIN firs f ON f.id = fa.fir_id
LEFT JOIN users u ON u.id = f.filed_by_user_id
WHERE f.filed_by_user_id IS NOT NULL
  AND u.id IS NULL;

DELETE fu
FROM fir_updates fu
JOIN firs f ON f.id = fu.fir_id
LEFT JOIN users u ON u.id = f.filed_by_user_id
WHERE f.filed_by_user_id IS NOT NULL
  AND u.id IS NULL;

DELETE ef
FROM evidence_files ef
JOIN firs f ON f.id = ef.fir_id
LEFT JOIN users u ON u.id = f.filed_by_user_id
WHERE f.filed_by_user_id IS NOT NULL
  AND u.id IS NULL;

DELETE f
FROM firs f
LEFT JOIN users u ON u.id = f.filed_by_user_id
WHERE f.filed_by_user_id IS NOT NULL
  AND u.id IS NULL;
