package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invalidated_tokens")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvalidatedTokenEntity {
    @Id
    String id;
    Date expiryTime;
}
