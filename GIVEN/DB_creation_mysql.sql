-- 1. Create database
CREATE DATABASE eventicketingsystem;

-- 2. Use the database
CREATE USER 'eventicketingsystemuser'@'%' IDENTIFIED BY 'print';

-- 3. Grant privileges on the database
GRANT ALL PRIVILEGES ON eventicketingsystem.* TO 'eventicketingsystemuser'@'%';

-- 4. Apply changes
FLUSH PRIVILEGES;