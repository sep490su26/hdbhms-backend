package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "deposit_form_co_occupants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_deposit_form_co_occupant_order", columnNames = {"deposit_form_id", "display_order"})
        },
        indexes = {
                @Index(name = "idx_deposit_form_co_occupants_form", columnList = "deposit_form_id")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositFormCoOccupantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposit_form_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    DepositFormEntity depositForm;

    @Column(name = "full_name", length = 255, nullable = false)
    String fullName;

    @Column(name = "phone", length = 30, nullable = false)
    String phone;

    @Column(name = "display_order", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    Integer displayOrder;
}
