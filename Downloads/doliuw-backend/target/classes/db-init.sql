-- ============================================================
-- Doliuw App – MySQL Database Setup
-- Run this ONCE before starting the backend for the first time
-- ============================================================

CREATE DATABASE IF NOT EXISTS doliuw_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE doliuw_db;

-- Hibernate's ddl-auto=update will create all tables automatically.
-- This script just ensures the DB and user exist.

-- (Optional) Create a dedicated app user instead of using root:
-- CREATE USER IF NOT EXISTS 'doliuw_user'@'localhost' IDENTIFIED BY 'StrongPassword123!';
-- GRANT ALL PRIVILEGES ON doliuw_db.* TO 'doliuw_user'@'localhost';
-- FLUSH PRIVILEGES;

-- After Hibernate auto-creates tables, you can verify with:
-- SHOW TABLES;
-- DESCRIBE users;
-- DESCRIBE user_progress;
-- DESCRIBE bookings;
-- DESCRIBE otp_store;

-- ============================================================
-- STEP 2: After starting the backend (Hibernate creates tables),
-- run the question seed to populate 1000+ questions:
-- ============================================================
-- source /path/to/questions-seed.sql
-- OR from MySQL CLI:
-- mysql -u root -p doliuw_db < questions-seed.sql

-- ============================================================
-- ENCRYPTION NOTE:
-- Sensitive user fields (name, avatar) are stored AES-256-GCM
-- encrypted in the database. Only id and email are in plaintext.
-- The encryption key is in application.properties:
--   app.encryption.secret=<32-byte base64 key>
-- In production, set via environment variable:
--   export APP_ENCRYPTION_SECRET=<your-key>
-- Generate a new key: openssl rand -base64 32
-- ============================================================
