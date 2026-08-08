package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.property.domain.value_objects.MeterStatus;
import com.sep490.hdbhms.property.domain.value_objects.MeterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaLeaseContractRepository extends JpaRepository<LeaseContractEntity, Long>, JpaSpecificationExecutor<LeaseContractEntity> {
    List<LeaseContractEntity> findAllByPrimaryTenantProfile_Id(Long tenantPersonProfileId);

    boolean existsByRoom_IdAndStatusInAndDeletedAtIsNull(Long roomId, List<LeaseStatus> statuses);

    boolean existsByContractCodeAndDeletedAtIsNull(String contractCode);

    boolean existsByPreviousContract_IdAndDeletedAtIsNull(Long previousContractId);

    boolean existsByPreviousContract_IdAndStatusAndDeletedAtIsNull(
            Long previousContractId,
            LeaseStatus status
    );

    Optional<LeaseContractEntity> findFirstByPreviousContract_IdAndDeletedAtIsNullOrderByIdDesc(Long previousContractId);

    List<LeaseContractEntity> findAllByStatusInAndDeletedAtIsNull(List<LeaseStatus> statuses);

    @Query("""
            SELECT DISTINCT contract FROM LeaseContractEntity contract
            JOIN FETCH contract.room room
            JOIN FETCH room.property
            LEFT JOIN FETCH contract.primaryTenantProfile primaryTenantProfile
            LEFT JOIN FETCH primaryTenantProfile.user
            WHERE contract.deletedAt IS NULL
              AND contract.id = :contractId
            """)
    Optional<LeaseContractEntity> findByIdForLeaseExpiryReminder(@Param("contractId") Long contractId);

    @Query("""
            SELECT DISTINCT contract FROM LeaseContractEntity contract
            JOIN FETCH contract.room room
            JOIN FETCH room.property
            LEFT JOIN FETCH contract.primaryTenantProfile primaryTenantProfile
            LEFT JOIN FETCH primaryTenantProfile.user
            WHERE contract.deletedAt IS NULL
              AND contract.status IN :statuses
            """)
    List<LeaseContractEntity> findLifecycleCandidates(@Param("statuses") List<LeaseStatus> statuses);

    @Query("""
            SELECT DISTINCT contract FROM LeaseContractEntity contract
            JOIN FETCH contract.room room
            JOIN FETCH room.property
            LEFT JOIN FETCH contract.primaryTenantProfile primaryTenantProfile
            LEFT JOIN FETCH primaryTenantProfile.user
            WHERE contract.deletedAt IS NULL
              AND contract.id = :contractId
              AND contract.status IN :statuses
            """)
    Optional<LeaseContractEntity> findLifecycleCandidateById(
            @Param("contractId") Long contractId,
            @Param("statuses") List<LeaseStatus> statuses
    );

    Optional<LeaseContractEntity> findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(
            Long roomId,
            List<LeaseStatus> statuses
    );

    @Query("""
            SELECT DISTINCT room FROM LeaseContractEntity contract
            JOIN contract.room room
            LEFT JOIN ContractLiquidationEntity liquidation
              ON liquidation.contract.id = contract.id
             AND liquidation.status = com.sep490.hdbhms.occupancy.domain.value_objects.LiquidationStatus.CONFIRMED
            WHERE contract.deletedAt IS NULL
              AND room.deletedAt IS NULL
              AND contract.status IN :statuses
              AND (:propertyId IS NULL OR room.property.id = :propertyId)
              AND COALESCE(contract.rentStartDate, contract.startDate) <= :periodEnd
              AND (COALESCE(liquidation.liquidationDate, contract.endDate) IS NULL
                   OR COALESCE(liquidation.liquidationDate, contract.endDate) >= :periodStart)
            ORDER BY room.sortOrder ASC, room.roomCode ASC
            """)
    List<RoomEntity> findMeterReadingRoomsByPeriod(
            @Param("propertyId") Long propertyId,
            @Param("statuses") List<LeaseStatus> statuses,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    @Query("""
            SELECT DISTINCT room FROM LeaseContractEntity contract
            JOIN contract.room room
            JOIN MeterEntity meter
              ON meter.room.id = room.id
             AND meter.meterType = :meterType
             AND meter.status = :meterStatus
            LEFT JOIN ContractLiquidationEntity liquidation
              ON liquidation.contract.id = contract.id
             AND liquidation.status = com.sep490.hdbhms.occupancy.domain.value_objects.LiquidationStatus.CONFIRMED
            WHERE contract.deletedAt IS NULL
              AND room.deletedAt IS NULL
              AND contract.status IN :statuses
              AND (:propertyId IS NULL OR room.property.id = :propertyId)
              AND COALESCE(contract.rentStartDate, contract.startDate) <= :periodEnd
              AND (COALESCE(liquidation.liquidationDate, contract.endDate) IS NULL
                   OR COALESCE(liquidation.liquidationDate, contract.endDate) >= :periodStart)
            ORDER BY room.sortOrder ASC, room.roomCode ASC
            """)
    List<RoomEntity> findMeterReadingRoomsByPeriodWithActiveMeter(
            @Param("propertyId") Long propertyId,
            @Param("statuses") List<LeaseStatus> statuses,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("meterType") MeterType meterType,
            @Param("meterStatus") MeterStatus meterStatus
    );

    @Query("""
            SELECT COUNT(DISTINCT room.id) FROM LeaseContractEntity contract
            JOIN contract.room room
            LEFT JOIN ContractLiquidationEntity liquidation
              ON liquidation.contract.id = contract.id
             AND liquidation.status = com.sep490.hdbhms.occupancy.domain.value_objects.LiquidationStatus.CONFIRMED
            WHERE contract.deletedAt IS NULL
              AND room.deletedAt IS NULL
              AND contract.status IN :statuses
              AND (:propertyId IS NULL OR room.property.id = :propertyId)
              AND COALESCE(contract.rentStartDate, contract.startDate) <= :periodEnd
              AND (COALESCE(liquidation.liquidationDate, contract.endDate) IS NULL
                   OR COALESCE(liquidation.liquidationDate, contract.endDate) >= :periodStart)
            """)
    long countMeterReadingRoomsByPeriod(
            @Param("propertyId") Long propertyId,
            @Param("statuses") List<LeaseStatus> statuses,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    @Query("""
            SELECT COUNT(DISTINCT room.id) FROM LeaseContractEntity contract
            JOIN contract.room room
            JOIN MeterEntity meter
              ON meter.room.id = room.id
             AND meter.meterType = :meterType
             AND meter.status = :meterStatus
            LEFT JOIN ContractLiquidationEntity liquidation
              ON liquidation.contract.id = contract.id
             AND liquidation.status = com.sep490.hdbhms.occupancy.domain.value_objects.LiquidationStatus.CONFIRMED
            WHERE contract.deletedAt IS NULL
              AND room.deletedAt IS NULL
              AND contract.status IN :statuses
              AND (:propertyId IS NULL OR room.property.id = :propertyId)
              AND COALESCE(contract.rentStartDate, contract.startDate) <= :periodEnd
              AND (COALESCE(liquidation.liquidationDate, contract.endDate) IS NULL
                   OR COALESCE(liquidation.liquidationDate, contract.endDate) >= :periodStart)
            """)
    long countMeterReadingRoomsByPeriodWithActiveMeter(
            @Param("propertyId") Long propertyId,
            @Param("statuses") List<LeaseStatus> statuses,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("meterType") MeterType meterType,
            @Param("meterStatus") MeterStatus meterStatus
    );

    @Query("""
            SELECT COUNT(contract) FROM LeaseContractEntity contract
            JOIN contract.room room
            LEFT JOIN ContractLiquidationEntity liquidation
              ON liquidation.contract.id = contract.id
             AND liquidation.status = com.sep490.hdbhms.occupancy.domain.value_objects.LiquidationStatus.CONFIRMED
            WHERE contract.deletedAt IS NULL
              AND room.deletedAt IS NULL
              AND contract.status IN :statuses
              AND room.id = :roomId
              AND room.property.id = :propertyId
              AND COALESCE(contract.rentStartDate, contract.startDate) <= :periodEnd
              AND (COALESCE(liquidation.liquidationDate, contract.endDate) IS NULL
                   OR COALESCE(liquidation.liquidationDate, contract.endDate) >= :periodStart)
            """)
    long countMeterReadingRoomContractsByPeriod(
            @Param("propertyId") Long propertyId,
            @Param("roomId") Long roomId,
            @Param("statuses") List<LeaseStatus> statuses,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    @Query("""
            SELECT COUNT(DISTINCT contract.id) FROM LeaseContractEntity contract
            JOIN contract.room room
            JOIN MeterEntity meter
              ON meter.room.id = room.id
             AND meter.meterType = :meterType
             AND meter.status = :meterStatus
            LEFT JOIN ContractLiquidationEntity liquidation
              ON liquidation.contract.id = contract.id
             AND liquidation.status = com.sep490.hdbhms.occupancy.domain.value_objects.LiquidationStatus.CONFIRMED
            WHERE contract.deletedAt IS NULL
              AND room.deletedAt IS NULL
              AND contract.status IN :statuses
              AND room.id = :roomId
              AND room.property.id = :propertyId
              AND COALESCE(contract.rentStartDate, contract.startDate) <= :periodEnd
              AND (COALESCE(liquidation.liquidationDate, contract.endDate) IS NULL
                   OR COALESCE(liquidation.liquidationDate, contract.endDate) >= :periodStart)
            """)
    long countMeterReadingRoomContractsByPeriodWithActiveMeter(
            @Param("propertyId") Long propertyId,
            @Param("roomId") Long roomId,
            @Param("statuses") List<LeaseStatus> statuses,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("meterType") MeterType meterType,
            @Param("meterStatus") MeterStatus meterStatus
    );

    @Query("""
            SELECT contract FROM LeaseContractEntity contract
            JOIN contract.room room
            LEFT JOIN ContractLiquidationEntity liquidation
              ON liquidation.contract.id = contract.id
             AND liquidation.status = com.sep490.hdbhms.occupancy.domain.value_objects.LiquidationStatus.CONFIRMED
            WHERE contract.deletedAt IS NULL
              AND room.deletedAt IS NULL
              AND contract.status IN :statuses
              AND room.id = :roomId
              AND COALESCE(contract.rentStartDate, contract.startDate) <= :periodEnd
              AND (COALESCE(liquidation.liquidationDate, contract.endDate) IS NULL
                   OR COALESCE(liquidation.liquidationDate, contract.endDate) >= :periodStart)
            ORDER BY contract.id DESC
            """)
    List<LeaseContractEntity> findMeterReadingContractsByRoomAndPeriod(
            @Param("roomId") Long roomId,
            @Param("statuses") List<LeaseStatus> statuses,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    boolean existsByPrimaryTenantProfile_Id(Long tenantProfileId);

    Optional<LeaseContractEntity> findByIdAndDeletedAtIsNull(Long id);

    Optional<LeaseContractEntity> findFirstByDepositForm_IdAndDeletedAtIsNull(Long depositFormId);
}
