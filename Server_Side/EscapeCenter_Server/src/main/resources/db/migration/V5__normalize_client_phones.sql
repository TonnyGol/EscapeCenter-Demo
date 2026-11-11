-- Same rule as BookingService.normalizedPhone: digits only, keeping a leading '+'.
-- Fails on purpose if two stored numbers collapse into one, so someone merges those clients by hand.
UPDATE clients
SET phone = CONCAT(IF(LEFT(TRIM(phone), 1) = '+', '+', ''), REGEXP_REPLACE(phone, '[^0-9]', ''));
