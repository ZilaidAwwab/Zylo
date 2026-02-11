package com.example.zylo.device.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "device", indexes = {
        @Index(name = "idx_device_id", columnList = "device_id"),
        @Index(name = "idx_type", columnList = "device_type"),
        @Index(name = "idx_active", columnList = "is_active"),
        @Index(name = "idx_location", columnList = "location")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 'KIOSK-001, POS-STORE-A-01'
    @Column(name = "device_id", unique = true, nullable = false, length = 100)
    private String deviceId;

    // 'KIOSK', 'POS', 'VENDING', 'MOBILE_APP'
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    @Column(length = 255)
    private String location;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "mqtt_topic")
    private String mqttTopic;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "mac_address", length = 17)
    private String macAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DeviceType {
        KIOSK, POS, VENDING, MOBILE_APP
    }
}
