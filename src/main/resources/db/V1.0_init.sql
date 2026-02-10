-- ============================================================================
-- DATABASE SCHEMA
-- ============================================================================

CREATE DATABASE zylo;

USE zylo;

-- Drop existing tables (for clean setup)
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS product_specs;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS address;
DROP TABLE IF EXISTS device;
DROP TABLE IF EXISTS user;

-- ============================================================================
-- USER MANAGEMENT
-- ============================================================================

CREATE TABLE user (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- Identity
                      -- since we are only using cognito so we can make the cognito_sub NOT NULL
                      cognito_sub VARCHAR(255) UNIQUE NOT NULL COMMENT 'AWS Cognito user identifier',
                      email VARCHAR(255) UNIQUE NOT NULL,
                      name VARCHAR(100) NOT NULL,
                      phone_no VARCHAR(20),

    -- Authentication
                      password_hash VARCHAR(255) COMMENT 'Null for Cognito-only users',
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
                      INDEX idx_cognito_sub (cognito_sub),
                      INDEX idx_phone (phone_no),
                      INDEX idx_role (role)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User accounts with dual auth support (Cognito + traditional)';

-- ============================================================================
-- ADDRESS MANAGEMENT
-- ============================================================================

CREATE TABLE address (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         user_id BIGINT NOT NULL,

    -- Address Details
                         label VARCHAR(50), -- 'Home, Work, Other',
                         address_line1 VARCHAR(255) NOT NULL,   -- House No
                         address_line2 VARCHAR(255),            -- Street
                         city VARCHAR(100) NOT NULL,
                         state VARCHAR(100) NOT NULL,
                         country VARCHAR(100) NOT NULL DEFAULT 'Pakistan',
                         postal_code VARCHAR(20) NOT NULL,

    -- Preferences
                         is_default BOOLEAN DEFAULT FALSE,  -- can specify their 'Home', 'Work', other address as default

    -- Audit
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign Keys
                         FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,

    -- Indexes
                         INDEX idx_user_id (user_id),
                         INDEX idx_default (user_id, is_default)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User delivery addresses';

-- ============================================================================
-- PRODUCT CATALOG
-- ============================================================================

CREATE TABLE category (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,

                          name VARCHAR(100) UNIQUE NOT NULL,
                          slug VARCHAR(100) UNIQUE NOT NULL,
                          description TEXT,
                          parent_id BIGINT COMMENT 'For nested categories',

    -- Metadata
                          is_active BOOLEAN DEFAULT TRUE,
                          display_order INT DEFAULT 0,

                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL,
                          INDEX idx_slug (slug),
                          INDEX idx_active (is_active)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Product categories (e.g., Laptops, Smartphones)';

CREATE TABLE product (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- Identity
                         sku VARCHAR(50) UNIQUE NOT NULL COMMENT 'Stock Keeping Unit',
                         name VARCHAR(255) NOT NULL,
                         category_id BIGINT,
                         description TEXT,

    -- Pricing
                         base_price DECIMAL(10, 2) NOT NULL,

    -- Stock Management
                         stock_quantity INT DEFAULT 0,
                         stock_status ENUM('IN_STOCK', 'OUT_OF_STOCK', 'LOW_STOCK')
        GENERATED ALWAYS AS (
            CASE
                WHEN stock_quantity > 10 THEN 'IN_STOCK'
                WHEN stock_quantity > 0 THEN 'LOW_STOCK'
                ELSE 'OUT_OF_STOCK'
            END
        ) STORED,
                         low_stock_threshold INT DEFAULT 10,

    -- Sales Metrics
                         items_sold INT DEFAULT 0,
                         average_rating DECIMAL(3, 2) DEFAULT 0.00,
                         total_reviews INT DEFAULT 0,

    -- Media
                         primary_image_url VARCHAR(500),
                         additional_images JSON COMMENT 'Array of image URLs',

    -- Status
                         is_active BOOLEAN DEFAULT TRUE,

    -- Audit
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Creator Identity
                         created_by BIGINT COMMENT 'Admin who created this product',
                         updated_by BIGINT COMMENT 'Admin who last updated',

                         FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL,

                         INDEX idx_category (category_id),
                         INDEX idx_sku (sku),
                         INDEX idx_active (is_active),
                         INDEX idx_stock (stock_status),
                         INDEX idx_rating (average_rating),
                         INDEX idx_price (base_price)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Product master data';

CREATE TABLE product_specs (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               product_id BIGINT UNIQUE NOT NULL COMMENT '1:1 relationship',

    -- Common Specs
                               brand VARCHAR(100) DEFAULT 'Apple',
                               model_year YEAR,
                               warranty_months INT DEFAULT 12,

    -- Tech Specs (Apple Products)
                               processor VARCHAR(100) COMMENT 'M2, M3 Pro, M3 Max, A17 Pro',
                               ram VARCHAR(20) COMMENT '8GB, 16GB, 32GB, 64GB',
                               storage VARCHAR(20) COMMENT '256GB, 512GB, 1TB, 2TB',

    -- Display
                               screen_size VARCHAR(20) COMMENT '13.3", 14.2", 16.2", 6.1"',
                               screen_resolution VARCHAR(50) COMMENT '2556x1179, 2796x1290',
                               screen_type VARCHAR(50) COMMENT 'Liquid Retina XDR, ProMotion',

    -- Design
                               color VARCHAR(50) COMMENT 'Space Gray, Silver, Midnight, Starlight',
                               weight_grams INT,
                               dimensions VARCHAR(100) COMMENT 'L x W x H in mm',

    -- Camera (for iPhones/iPads)
                               camera_system VARCHAR(200) COMMENT '48MP Main, 12MP Ultra Wide',
                               front_camera VARCHAR(100) COMMENT '12MP TrueDepth',

    -- Battery
                               battery_capacity VARCHAR(50) COMMENT '3200mAh',
                               battery_life VARCHAR(100) COMMENT 'Up to 22 hours video playback',

    -- Connectivity
                               connectivity JSON COMMENT '["5G", "WiFi 6E", "Bluetooth 5.3"]',
                               ports JSON COMMENT '["Thunderbolt 4", "MagSafe 3", "SDXC"]',

    -- Additional Features
                               features JSON COMMENT 'Face ID, Touch ID, Spatial Audio, etc.',

    -- Status
                               is_active BOOLEAN DEFAULT TRUE,

                               FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
                               INDEX idx_product_id (product_id),
                               INDEX idx_ram (ram),
                               INDEX idx_storage (storage),
                               INDEX idx_color (color)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Product technical specifications';

-- ============================================================================
-- SHOPPING CART
-- ============================================================================

CREATE TABLE cart (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      user_id BIGINT UNIQUE NOT NULL COMMENT '1:1 with user',

                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                      FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                      INDEX idx_user_id (user_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User shopping carts';

CREATE TABLE cart_item (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           cart_id BIGINT NOT NULL,
                           product_id BIGINT NOT NULL,

                           quantity INT DEFAULT 1 CHECK (quantity > 0),

                           added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                           FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
                           FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,

                           UNIQUE KEY unique_cart_product (cart_id, product_id),
                           INDEX idx_cart_id (cart_id),
                           INDEX idx_product_id (product_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Items in shopping cart';

-- ============================================================================
-- DEVICE MANAGEMENT (IoT/MQTT)
-- ============================================================================

CREATE TABLE device (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- Identity
                        device_id VARCHAR(100) UNIQUE NOT NULL COMMENT 'KIOSK-001, POS-STORE-A-01',
                        device_type ENUM('KIOSK', 'POS', 'VENDING', 'MOBILE_APP') NOT NULL,

    -- Location
                        location VARCHAR(255) COMMENT 'Mall-A, Store-B, City Center',
                        latitude DECIMAL(10, 8),
                        longitude DECIMAL(11, 8),

    -- MQTT Configuration
                        mqtt_topic VARCHAR(255) COMMENT 'device/KIOSK-001/orders',

    -- Status
                        is_active BOOLEAN DEFAULT TRUE,
                        last_seen_at TIMESTAMP,

    -- Metadata
                        firmware_version VARCHAR(50),
                        ip_address VARCHAR(45),
                        mac_address VARCHAR(17),

    -- Audit
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                        INDEX idx_device_id (device_id),
                        INDEX idx_type (device_type),
                        INDEX idx_active (is_active),
                        INDEX idx_location (location)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='IoT devices posting orders via MQTT';

-- ============================================================================
-- ORDER MANAGEMENT
-- ============================================================================

CREATE TABLE `order` (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- Identity
                         order_number VARCHAR(50) UNIQUE NOT NULL COMMENT 'ORD-20240210-001234',

    -- References
                         user_id BIGINT NOT NULL,
                         address_id BIGINT NOT NULL,
                         device_id BIGINT COMMENT 'Null for web/mobile orders',

    -- Pricing
                         sub_total DECIMAL(10, 2) NOT NULL,
                         tax DECIMAL(10, 2) DEFAULT 0.00,
                         discount DECIMAL(10, 2) DEFAULT 0.00,
                         shipping_fee DECIMAL(10, 2) DEFAULT 0.00,
                         total_amount DECIMAL(10, 2) NOT NULL,

    -- Order Status
                         order_status ENUM(
        'PENDING',
        'CONFIRMED',
        'PROCESSING',
        'SHIPPED',
        'DELIVERED',
        'CANCELLED'
    ) DEFAULT 'PENDING' NOT NULL,

    -- Payment
                         payment_method ENUM('CARD', 'UPI', 'COD', 'WALLET') NOT NULL,
                         payment_status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')
        DEFAULT 'PENDING' NOT NULL,
                         payment_id VARCHAR(255) COMMENT 'Stripe/Razorpay transaction ID',

    -- Delivery
                         tracking_number VARCHAR(100),
                         estimated_delivery_date DATE,
                         delivered_at TIMESTAMP,

    -- Audit
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         cancelled_at TIMESTAMP,
                         cancellation_reason TEXT,

    -- Foreign Keys
                         FOREIGN KEY (user_id) REFERENCES user(id),
                         FOREIGN KEY (address_id) REFERENCES address(id),
                         FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE SET NULL,

    -- Indexes
                         INDEX idx_user_id (user_id),
                         INDEX idx_order_number (order_number),
                         INDEX idx_order_status (order_status),
                         INDEX idx_payment_status (payment_status),
                         INDEX idx_created_at (created_at),
                         INDEX idx_device_id (device_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Customer orders';

CREATE TABLE order_item (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            order_id BIGINT NOT NULL,
                            product_id BIGINT NOT NULL,

    -- Quantity & Pricing (snapshot at time of order)
                            quantity INT NOT NULL CHECK (quantity > 0),
                            unit_price DECIMAL(10, 2) NOT NULL COMMENT 'Price at time of order',
                            total_price DECIMAL(10, 2) NOT NULL COMMENT 'unit_price * quantity',

    -- Product Snapshot (in case product changes/deleted later)
                            product_name VARCHAR(255) NOT NULL,
                            product_sku VARCHAR(50),
                            product_specs JSON COMMENT 'Snapshot of specs at purchase time',

                            FOREIGN KEY (order_id) REFERENCES `order`(id) ON DELETE CASCADE,
                            FOREIGN KEY (product_id) REFERENCES product(id),

                            INDEX idx_order_id (order_id),
                            INDEX idx_product_id (product_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Line items in orders';

-- ============================================================================
-- SEED DATA (Sample Products)
-- ============================================================================

-- Categories
INSERT INTO category (name, slug, description, display_order) VALUES
                                                                  ('Laptops', 'laptops', 'MacBook Pro and MacBook Air', 1),
                                                                  ('Smartphones', 'smartphones', 'iPhone series', 2),
                                                                  ('Tablets', 'tablets', 'iPad series', 3);

-- Products
INSERT INTO product (sku, name, category_id, description, base_price, stock_quantity, primary_image_url) VALUES
-- MacBooks
('MBP14-M3PRO-2024', 'MacBook Pro 14" (M3 Pro)', 1,
 'The most powerful MacBook Pro ever, with M3 Pro chip',
 1999.00, 50, 'https://example.com/mbp14.jpg'),

('MBP16-M3MAX-2024', 'MacBook Pro 16" (M3 Max)', 1,
 'The ultimate pro notebook with M3 Max',
 2499.00, 30, 'https://example.com/mbp16.jpg'),

('MBA13-M3-2024', 'MacBook Air 13" (M3)', 1,
 'Supercharged by M3, incredibly thin and light',
 1299.00, 100, 'https://example.com/mba13.jpg'),

-- iPhones
('IPHONE15PRO-128', 'iPhone 15 Pro', 2,
 'Titanium. A17 Pro chip. Action button.',
 999.00, 200, 'https://example.com/iphone15pro.jpg'),

('IPHONE15PROMAX-256', 'iPhone 15 Pro Max', 2,
 'The ultimate iPhone with 6.7" display',
 1199.00, 150, 'https://example.com/iphone15promax.jpg');

-- Product Specs
INSERT INTO product_specs (product_id, processor, ram, storage, screen_size, color) VALUES
-- MacBook Pro 14" M3 Pro
(1, 'Apple M3 Pro (11-core CPU, 14-core GPU)', '16GB', '512GB', '14.2"', 'Space Black'),

-- MacBook Pro 16" M3 Max
(2, 'Apple M3 Max (14-core CPU, 30-core GPU)', '32GB', '1TB', '16.2"', 'Space Black'),

-- MacBook Air 13" M3
(3, 'Apple M3 (8-core CPU, 10-core GPU)', '8GB', '256GB', '13.6"', 'Midnight'),

-- iPhone 15 Pro
(4, 'A17 Pro', '8GB', '128GB', '6.1"', 'Natural Titanium'),

-- iPhone 15 Pro Max
(5, 'A17 Pro', '8GB', '256GB', '6.7"', 'Blue Titanium');

-- Sample Admin User (password: admin123)
-- Password hash: bcrypt of "admin123"
INSERT INTO user (email, name, cognito_sub, role, is_email_verified) VALUES
    ('admin@zylo.com', 'Zylo Admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', TRUE);

-- Sample Devices
INSERT INTO device (device_id, device_type, location, mqtt_topic, is_active) VALUES
                                                                                 ('KIOSK-MALL-A-01', 'KIOSK', 'Dolmen Mall Karachi', 'device/KIOSK-MALL-A-01/orders', TRUE),
                                                                                 ('KIOSK-MALL-A-02', 'KIOSK', 'Dolmen Mall Karachi', 'device/KIOSK-MALL-A-02/orders', TRUE),
                                                                                 ('POS-STORE-B-01', 'POS', 'Liberty Books Karachi', 'device/POS-STORE-B-01/orders', TRUE);
