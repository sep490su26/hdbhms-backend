//package com.sep490.hdbhms.modules.tenant.entity;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "tenants")
//public class Tenant {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "user_id", nullable = false)
//    private Long userId;
//
//    @Column(name = "property_id", nullable = false)
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
//    public Long getPropertyId() {
//        return propertyId;
//    }
//
//    public LocalDateTime getDeletedAt() {
//        return deletedAt;
//    }
//}
