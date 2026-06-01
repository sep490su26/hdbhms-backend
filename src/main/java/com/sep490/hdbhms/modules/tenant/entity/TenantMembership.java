//package com.sep490.hdbhms.modules.tenant.entity;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.EnumType;
//import jakarta.persistence.Enumerated;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "role_promotions")
//public class TenantMembership {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "user_id", nullable = false)
//    private Long userId;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private TenantRole role;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private TenantMembershipStatus status;
//
//    @Column(name = "property_id")
//    private Long propertyId;
//
//    @Column(name = "deleted_at")
//    private LocalDateTime deletedAt;
//
//    public Long getId() {
//        return id;
//    }
//
//    public Long getUserId() {
//        return userId;
//    }
//
//    public TenantRole getRole() {
//        return role;
//    }
//
//    public TenantMembershipStatus getStatus() {
//        return status;
//    }
//
//    public Long getPropertyId() {
//        return propertyId;
//    }
//
//    public LocalDateTime getDeletedAt() {
//        return deletedAt;
//    }
//}
