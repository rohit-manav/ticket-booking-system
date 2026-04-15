-- Bootstrap script: roles, permissions, mappings, and first admin role assignment
-- Target DB: MySQL
-- Usage:
-- 1) Register admin user via API first (to ensure BCrypt password hashing), e.g. admin@example.com
-- 2) Run this script

USE eventicketingsystem;

-- -----------------------------------------------------------------------------
-- Roles
-- -----------------------------------------------------------------------------
INSERT INTO roles (name, created_at, updated_at)
SELECT 'ADMIN', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

INSERT INTO roles (name, created_at, updated_at)
SELECT 'CUSTOMER', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'CUSTOMER');

-- -----------------------------------------------------------------------------
-- Permissions
-- -----------------------------------------------------------------------------
INSERT INTO permissions (name, created_at, updated_at)
SELECT 'CREATE_EVENT', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CREATE_EVENT');

INSERT INTO permissions (name, created_at, updated_at)
SELECT 'UPDATE_EVENT', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'UPDATE_EVENT');

INSERT INTO permissions (name, created_at, updated_at)
SELECT 'DELETE_EVENT', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'DELETE_EVENT');

INSERT INTO permissions (name, created_at, updated_at)
SELECT 'MANAGE_SEATS', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MANAGE_SEATS');

INSERT INTO permissions (name, created_at, updated_at)
SELECT 'VIEW_ALL_BOOKINGS', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'VIEW_ALL_BOOKINGS');

INSERT INTO permissions (name, created_at, updated_at)
SELECT 'MANAGE_USERS', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MANAGE_USERS');

INSERT INTO permissions (name, created_at, updated_at)
SELECT 'MANAGE_ROLES', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MANAGE_ROLES');

INSERT INTO permissions (name, created_at, updated_at)
SELECT 'MANAGE_PERMISSIONS', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MANAGE_PERMISSIONS');

INSERT INTO permissions (name, created_at, updated_at)
SELECT 'VIEW_EVENTS', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'VIEW_EVENTS');

INSERT INTO permissions (name, created_at, updated_at)
SELECT 'VIEW_SEATS', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'VIEW_SEATS');

INSERT INTO permissions (name, created_at, updated_at)
SELECT 'BOOK_SEAT', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'BOOK_SEAT');

INSERT INTO permissions (name, created_at, updated_at)
SELECT 'MANAGE_OWN_BOOKINGS', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MANAGE_OWN_BOOKINGS');

-- -----------------------------------------------------------------------------
-- Role-Permission mappings
-- -----------------------------------------------------------------------------
-- ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- CUSTOMER gets limited permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('VIEW_EVENTS', 'VIEW_SEATS', 'BOOK_SEAT', 'MANAGE_OWN_BOOKINGS')
WHERE r.name = 'CUSTOMER'
  AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- -----------------------------------------------------------------------------
-- First admin bootstrap assignment
-- NOTE: Ensure this user exists already (create via register API first).
-- -----------------------------------------------------------------------------
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.email = 'admin@example.com'
  AND NOT EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- -----------------------------------------------------------------------------
-- Verification queries (optional)
-- -----------------------------------------------------------------------------
-- SELECT id, name FROM roles;
-- SELECT id, name FROM permissions;
-- SELECT u.id, u.email, r.name AS role_name
-- FROM users u
-- JOIN user_roles ur ON ur.user_id = u.id
-- JOIN roles r ON r.id = ur.role_id
-- WHERE u.email = 'admin@example.com';

