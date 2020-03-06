-- add actual qr code storage columns
ALTER TABLE badges ADD COLUMN qr_type TEXT;
ALTER TABLE badges ADD COLUMN qr_code TEXT;