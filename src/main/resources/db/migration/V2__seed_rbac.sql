-- Seed RBAC reference data (idempotent)

-- Roles
INSERT INTO roles (name, created_at, updated_at, deleted)
SELECT 'ADMIN', NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

INSERT INTO roles (name, created_at, updated_at, deleted)
SELECT 'CUSTOMER', NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'CUSTOMER');

-- Permissions
INSERT INTO permissions (name, created_at, updated_at, deleted)
SELECT 'event.create', NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'event.create');

INSERT INTO permissions (name, created_at, updated_at, deleted)
SELECT 'event.update', NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'event.update');

INSERT INTO permissions (name, created_at, updated_at, deleted)
SELECT 'event.delete', NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'event.delete');

INSERT INTO permissions (name, created_at, updated_at, deleted)
SELECT 'event.read', NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'event.read');

INSERT INTO permissions (name, created_at, updated_at, deleted)
SELECT 'seat.create', NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'seat.create');

INSERT INTO permissions (name, created_at, updated_at, deleted)
SELECT 'seat.update', NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'seat.update');

INSERT INTO permissions (name, created_at, updated_at, deleted)
SELECT 'seat.read', NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'seat.read');

INSERT INTO permissions (name, created_at, updated_at, deleted)
SELECT 'booking', NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'booking');

INSERT INTO permissions (name, created_at, updated_at, deleted)
SELECT 'profile', NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'profile');

-- Optional admin user bootstrap
INSERT INTO users (name, email, password, created_at, updated_at, deleted)
SELECT 'Admin', 'admin@example.com',
       '$2b$12$56UcNt6H.VV9gpDW39qTEeli5o5IQRVwZ4xDSKXNklOTIE7swwQki',
       NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@example.com');

-- ADMIN role gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
  );

-- CUSTOMER role gets limited permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('event.read', 'seat.read', 'booking')
WHERE r.name = 'CUSTOMER'
  AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
  );

-- Map bootstrap admin user to ADMIN role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.email = 'admin@example.com'
  AND NOT EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = u.id
      AND ur.role_id = r.id
  );
