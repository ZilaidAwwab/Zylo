-- Run commands to alter the User table
ALTER TABLE user
    -- 1. Remove Cognito column and its associated index
    DROP INDEX idx_cognito_sub,
    DROP COLUMN cognito_sub,

    -- 2. Remove the specific index on phone_no (as it's not in your Entity anymore)
    DROP INDEX idx_phone,

    -- 3. Update password_hash to NOT NULL to match your Java @Column(nullable = false)
    MODIFY COLUMN password_hash VARCHAR(255) NOT NULL,

    -- 4. Clean up the table comment
    COMMENT='User accounts with traditional authentication';

-- Or recreate the table
CREATE TABLE user (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- Identity
    -- since we are only using cognito so we can make the cognito_sub NOT NULL
                      email VARCHAR(255) UNIQUE NOT NULL,
                      name VARCHAR(100) NOT NULL,
                      phone_no VARCHAR(20),

    -- Authentication
                      password_hash VARCHAR(255) NOT NULL,
                      is_email_verified BOOLEAN DEFAULT FALSE,

    -- Authorization
                      role ENUM('CUSTOMER', 'ADMIN') DEFAULT 'CUSTOMER' NOT NULL,

    -- Password Reset (these would be null for cognito users)
                      reset_token VARCHAR(255),
                      reset_token_expiry TIMESTAMP,

    -- Audit
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      last_login_at TIMESTAMP,
                      is_deleted BOOLEAN DEFAULT FALSE,
                      deleted_at TIMESTAMP,

    -- Indexes
                      INDEX idx_email (email),
                      INDEX idx_role (role)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='User accounts with traditional authentication';
