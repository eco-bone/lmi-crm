-- ============================================================
-- LMI CRM — full wipe + super admin seed
-- Run this against your PostgreSQL database
-- ============================================================

-- Wipe all tables (CASCADE handles FK order automatically)
TRUNCATE TABLE
    audit_logs,
    alerts,
    group_prospects,
    "groups",
    meetings,
    prospect_licensees,
    prospects,
    resources,
    user_items,
    otp_store,
    licensee_cities,
    users
RESTART IDENTITY CASCADE;

-- Seed super admin
INSERT INTO users (
    first_name,
    last_name,
    email,
    phone,
    password,
    role,
    status,
    invite_email_sent
) VALUES (
    'Ajay',
    'Nagpal',
    'nagpalajay01@gmail.com',
    '9560057313',
    '$2b$10$4X969T5L5HkYswaTyXKmgOzDWGgUh9uzzvQoh1fal4XCftOVtuvOq',
    'SUPER_ADMIN',
    'ACTIVE',
    true
);
