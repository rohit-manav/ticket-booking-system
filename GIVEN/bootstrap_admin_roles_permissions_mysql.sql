-- Bootstrap script: roles, permissions, admin user, and role-permission mappings
-- Target DB: MySQL
-- Usage: Run this script after the application creates the schema (first startup).
--        Safe to run multiple times — deletes and recreates all seed data.
--
-- Default admin credentials:
--   Email:    admin@example.com
--   Password: Admin@1234

USE eventicketingsystem;

-- Disable safe update mode for this session (allows DELETE without key in WHERE)
SET SQL_SAFE_UPDATES = 0;

-- =============================================================================
-- CLEANUP: Delete ALL data from all tables (child tables first due to FK)
-- =============================================================================

-- Level 1: Leaf tables (no other table depends on these)
DELETE FROM booking_items WHERE id > 0 OR id = 0;

-- Level 2: Tables referenced only by booking_items (already cleared)
DELETE FROM bookings WHERE id > 0 OR id = 0;
DELETE FROM seats WHERE id > 0 OR id = 0;

-- Level 3: Join tables
DELETE FROM user_roles WHERE user_id > 0 OR user_id = 0;
DELETE FROM role_permissions WHERE role_id > 0 OR role_id = 0;

-- Level 4: Parent tables (no FK references left)
DELETE FROM events WHERE id > 0 OR id = 0;
DELETE FROM users WHERE id > 0 OR id = 0;
DELETE FROM permissions WHERE id > 0 OR id = 0;
DELETE FROM roles WHERE id > 0 OR id = 0;

-- =============================================================================
-- INSERT: Admin user (BCrypt hash of 'Admin@1234' with strength 12)
-- =============================================================================
INSERT INTO users (name, email, password, created_at, updated_at)
VALUES ('Admin', 'admin@example.com',
        '$2b$12$56UcNt6H.VV9gpDW39qTEeli5o5IQRVwZ4xDSKXNklOTIE7swwQki',
        NOW(), NOW());

-- =============================================================================
-- INSERT: Roles
-- =============================================================================
INSERT INTO roles (name, created_at, updated_at) VALUES ('ADMIN', NOW(), NOW());
INSERT INTO roles (name, created_at, updated_at) VALUES ('CUSTOMER', NOW(), NOW());

-- =============================================================================
-- INSERT: Permissions
-- =============================================================================
INSERT INTO permissions (name, created_at, updated_at) VALUES ('CREATE_EVENT', NOW(), NOW());
INSERT INTO permissions (name, created_at, updated_at) VALUES ('UPDATE_EVENT', NOW(), NOW());
INSERT INTO permissions (name, created_at, updated_at) VALUES ('DELETE_EVENT', NOW(), NOW());
INSERT INTO permissions (name, created_at, updated_at) VALUES ('MANAGE_SEATS', NOW(), NOW());
INSERT INTO permissions (name, created_at, updated_at) VALUES ('VIEW_ALL_BOOKINGS', NOW(), NOW());
INSERT INTO permissions (name, created_at, updated_at) VALUES ('MANAGE_USERS', NOW(), NOW());
INSERT INTO permissions (name, created_at, updated_at) VALUES ('MANAGE_ROLES', NOW(), NOW());
INSERT INTO permissions (name, created_at, updated_at) VALUES ('MANAGE_PERMISSIONS', NOW(), NOW());
INSERT INTO permissions (name, created_at, updated_at) VALUES ('VIEW_EVENTS', NOW(), NOW());
INSERT INTO permissions (name, created_at, updated_at) VALUES ('VIEW_SEATS', NOW(), NOW());
INSERT INTO permissions (name, created_at, updated_at) VALUES ('BOOK_SEAT', NOW(), NOW());
INSERT INTO permissions (name, created_at, updated_at) VALUES ('MANAGE_OWN_BOOKINGS', NOW(), NOW());

-- =============================================================================
-- MAP: Role-Permission mappings
-- =============================================================================
-- ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN';

-- CUSTOMER gets limited permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('VIEW_EVENTS', 'VIEW_SEATS', 'BOOK_SEAT', 'MANAGE_OWN_BOOKINGS')
WHERE r.name = 'CUSTOMER';

-- =============================================================================
-- MAP: Admin user role assignment (ADMIN only)
-- =============================================================================
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.email = 'admin@example.com';

-- =============================================================================
-- VERIFY
-- =============================================================================
SELECT '--- Roles ---' AS section;
SELECT id, name FROM roles;

SELECT '--- Permissions ---' AS section;
SELECT id, name FROM permissions;

SELECT '--- Role-Permission Mappings ---' AS section;
SELECT r.name AS role_name, GROUP_CONCAT(p.name ORDER BY p.name) AS permissions
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
GROUP BY r.name;

SELECT '--- Admin User ---' AS section;
SELECT u.id, u.name, u.email, GROUP_CONCAT(r.name) AS roles
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.email = 'admin@example.com'
GROUP BY u.id, u.name, u.email;

-- Re-enable safe update mode
SET SQL_SAFE_UPDATES = 1;

