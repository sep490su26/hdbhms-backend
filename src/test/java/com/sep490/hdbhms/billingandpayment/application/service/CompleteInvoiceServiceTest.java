package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.model.InvoiceLine;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.occupancy.application.port.in.command.AcceptHolderNominationCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CompleteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ConfirmTenantTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ExecuteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.NominateHolderCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.RoomTransferRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TransferSettlementRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.model.TransferSettlement;
import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.TransferOutUtilityEstimateResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CompleteInvoiceServiceTest {

    @Test
    void paidBatchDepositUsesBatchCompletionOnly() {
        AtomicInteger singleCalls = new AtomicInteger();
        AtomicInteger batchCalls = new AtomicInteger();
        CompleteInvoiceService service = new CompleteInvoiceService(
                invoice -> singleCalls.incrementAndGet(),
                invoice -> batchCalls.incrementAndGet(),
                new FailingTransferSettlementRepository(),
                new FailingRoomTransferRepository(),
                new FailingInvoiceLineRepository(),
                new FailingRoomTransferUseCase()
        );

        service.execute(Invoice.builder()
                .invoiceType(InvoiceType.DEPOSIT)
                .status(InvoiceStatus.PAID)
                .depositBatchId(12L)
                .build(), null);

        assertEquals(0, singleCalls.get());
        assertEquals(1, batchCalls.get());
    }

    @Test
    void paidSingleDepositKeepsExistingCompletionFlow() {
        AtomicInteger singleCalls = new AtomicInteger();
        AtomicInteger batchCalls = new AtomicInteger();
        CompleteInvoiceService service = new CompleteInvoiceService(
                invoice -> singleCalls.incrementAndGet(),
                invoice -> batchCalls.incrementAndGet(),
                new FailingTransferSettlementRepository(),
                new FailingRoomTransferRepository(),
                new FailingInvoiceLineRepository(),
                new FailingRoomTransferUseCase()
        );

        service.execute(Invoice.builder()
                .invoiceType(InvoiceType.DEPOSIT)
                .status(InvoiceStatus.PAID)
                .depositAgreementId(7L)
                .build(), null);

        assertEquals(1, singleCalls.get());
        assertEquals(0, batchCalls.get());
    }

    @Test
    void paidTransferDifferenceAutoConfirmsContractAfterPayment() {
        RoomTransferRequest request = RoomTransferRequest.builder()
                .id(9L)
                .status(TransferRequestStatus.WAITING_TENANT_CONFIRMATION)
                .build();
        RecordingRoomTransferRepository roomTransferRepository = new RecordingRoomTransferRepository(request);
        RecordingRoomTransferUseCase roomTransferUseCase = new RecordingRoomTransferUseCase();
        CompleteInvoiceService service = new CompleteInvoiceService(
                invoice -> { },
                invoice -> { },
                new TransferDifferenceSettlementRepository(77L, 9L, 13L),
                roomTransferRepository,
                new FailingInvoiceLineRepository(),
                roomTransferUseCase
        );

        service.execute(Invoice.builder()
                .id(77L)
                .invoiceType(InvoiceType.TRANSFER_DIFFERENCE)
                .status(InvoiceStatus.PAID)
                .build(), null);

        assertNull(roomTransferRepository.saved.get());
        assertEquals(9L, roomTransferUseCase.advancedRequestId.get());
        assertEquals(13L, roomTransferUseCase.advancedTenantUserId.get());
    }

    private record TransferDifferenceSettlementRepository(
            Long invoiceId,
            Long transferRequestId,
            Long confirmedById
    ) implements TransferSettlementRepository {
        @Override
        public TransferSettlement save(TransferSettlement transferSettlement) {
            throw unexpectedRepositoryCall("TransferSettlementRepository.save");
        }

        @Override
        public Optional<TransferSettlement> findLatestByTransferRequestId(Long transferRequestId) {
            throw unexpectedRepositoryCall("TransferSettlementRepository.findLatestByTransferRequestId");
        }

        @Override
        public Optional<TransferSettlement> findByTransferDifferenceInvoiceId(Long transferDifferenceInvoiceId) {
            if (!invoiceId.equals(transferDifferenceInvoiceId)) {
                return Optional.empty();
            }
            return Optional.of(TransferSettlement.builder()
                    .transferRequestId(transferRequestId)
                    .transferDifferenceInvoiceId(invoiceId)
                    .confirmedById(confirmedById)
                    .build());
        }
    }

    private static final class RecordingRoomTransferUseCase extends FailingRoomTransferUseCase {
        private final AtomicReference<Long> advancedRequestId = new AtomicReference<>();
        private final AtomicReference<Long> advancedTenantUserId = new AtomicReference<>();

        @Override
        public void advanceTransferAfterDifferencePayment(Long requestId, Long tenantUserId) {
            advancedRequestId.set(requestId);
            advancedTenantUserId.set(tenantUserId);
        }
    }

    private static final class RecordingRoomTransferRepository implements RoomTransferRepository {
        private final RoomTransferRequest request;
        private final AtomicReference<RoomTransferRequest> saved = new AtomicReference<>();

        private RecordingRoomTransferRepository(RoomTransferRequest request) {
            this.request = request;
        }

        @Override
        public RoomTransferRequest save(RoomTransferRequest roomTransferRequest) {
            saved.set(roomTransferRequest);
            return roomTransferRequest;
        }

        @Override
        public Optional<RoomTransferRequest> findById(Long id) {
            return request.getId().equals(id) ? Optional.of(request) : Optional.empty();
        }

        @Override
        public Optional<RoomTransferRequest> findByRequestCode(String requestCode) {
            throw unexpectedRepositoryCall("RoomTransferRepository.findByRequestCode");
        }

        @Override
        public List<RoomTransferRequest> findByStatusAndUpdatedAtBefore(
                TransferRequestStatus status,
                LocalDateTime updatedBefore) {
            throw unexpectedRepositoryCall("RoomTransferRepository.findByStatusAndUpdatedAtBefore");
        }

        @Override
        public List<RoomTransferRequest> findPendingHolderNominations(Long holderUserId) {
            throw unexpectedRepositoryCall("RoomTransferRepository.findPendingHolderNominations");
        }

        @Override
        public List<RoomTransferRequest> findPendingTargetHolderApprovals(Long holderUserId) {
            throw unexpectedRepositoryCall("RoomTransferRepository.findPendingTargetHolderApprovals");
        }
    }

    private static final class FailingTransferSettlementRepository implements TransferSettlementRepository {
        @Override
        public TransferSettlement save(TransferSettlement transferSettlement) {
            throw unexpectedRepositoryCall("TransferSettlementRepository.save");
        }

        @Override
        public Optional<TransferSettlement> findLatestByTransferRequestId(Long transferRequestId) {
            throw unexpectedRepositoryCall("TransferSettlementRepository.findLatestByTransferRequestId");
        }

        @Override
        public Optional<TransferSettlement> findByTransferDifferenceInvoiceId(Long transferDifferenceInvoiceId) {
            throw unexpectedRepositoryCall("TransferSettlementRepository.findByTransferDifferenceInvoiceId");
        }
    }

    private static final class FailingRoomTransferRepository implements RoomTransferRepository {
        @Override
        public RoomTransferRequest save(RoomTransferRequest roomTransferRequest) {
            throw unexpectedRepositoryCall("RoomTransferRepository.save");
        }

        @Override
        public Optional<RoomTransferRequest> findById(Long id) {
            throw unexpectedRepositoryCall("RoomTransferRepository.findById");
        }

        @Override
        public Optional<RoomTransferRequest> findByRequestCode(String requestCode) {
            throw unexpectedRepositoryCall("RoomTransferRepository.findByRequestCode");
        }

        @Override
        public List<RoomTransferRequest> findByStatusAndUpdatedAtBefore(
                TransferRequestStatus status,
                LocalDateTime updatedBefore) {
            throw unexpectedRepositoryCall("RoomTransferRepository.findByStatusAndUpdatedAtBefore");
        }

        @Override
        public List<RoomTransferRequest> findPendingHolderNominations(Long holderUserId) {
            throw unexpectedRepositoryCall("RoomTransferRepository.findPendingHolderNominations");
        }

        @Override
        public List<RoomTransferRequest> findPendingTargetHolderApprovals(Long holderUserId) {
            throw unexpectedRepositoryCall("RoomTransferRepository.findPendingTargetHolderApprovals");
        }
    }

    private static class FailingRoomTransferUseCase implements RoomTransferUseCase {
        @Override
        public Long createTransferRequest(CreateTransferRequestCommand command) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.createTransferRequest");
        }

        @Override
        public void nominateHolder(NominateHolderCommand command) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.nominateHolder");
        }

        @Override
        public void acceptHolderNomination(AcceptHolderNominationCommand command) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.acceptHolderNomination");
        }

        @Override
        public void rejectHolderNomination(Long requestId, Long tenantUserId) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.rejectHolderNomination");
        }

        @Override
        public void confirmTenantTransfer(ConfirmTenantTransferCommand command) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.confirmTenantTransfer");
        }

        @Override
        public void approveTransfer(ApproveTransferCommand command) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.approveTransfer");
        }

        @Override
        public void rejectTransferRequest(Long requestId, Long managerId, String resolutionNote) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.rejectTransferRequest");
        }

        @Override
        public void approveTargetHolderTransfer(Long requestId, Long holderUserId) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.approveTargetHolderTransfer");
        }

        @Override
        public void rejectTargetHolderTransfer(Long requestId, Long holderUserId) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.rejectTargetHolderTransfer");
        }

        @Override
        public void confirmTransferContract(Long requestId, Long tenantUserId) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.confirmTransferContract");
        }

        @Override
        public void advanceTransferAfterDifferencePayment(Long requestId, Long tenantUserId) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.advanceTransferAfterDifferencePayment");
        }

        @Override
        public void signTransferContract(Long requestId, Long tenantUserId) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.signTransferContract");
        }

        @Override
        public void rejectTransferContract(Long requestId, Long tenantUserId) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.rejectTransferContract");
        }

        @Override
        public void cancelTransferRequest(Long requestId) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.cancelTransferRequest");
        }

        @Override
        public void executeTransfer(ExecuteTransferCommand command) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.executeTransfer");
        }

        @Override
        public void completeTransfer(CompleteTransferCommand command) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.completeTransfer");
        }

        @Override
        public TransferOutUtilityEstimateResponse estimateTransferOutUtility(ExecuteTransferCommand command) {
            return null;
        }

        @Override
        public int expireTargetHolderApprovals() {
            throw unexpectedRepositoryCall("RoomTransferUseCase.expireTargetHolderApprovals");
        }

        @Override
        public int expireSourceHolderNominations() {
            throw unexpectedRepositoryCall("RoomTransferUseCase.expireSourceHolderNominations");
        }

        @Override
        public RoomTransferRequest getTransferRequestById(Long requestId) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.getTransferRequestById");
        }

        @Override
        public RoomTransferRequest getTransferRequestByCode(String requestCode) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.getTransferRequestByCode");
        }

        @Override
        public List<RoomTransferRequest> getPendingHolderNominations(Long holderUserId) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.getPendingHolderNominations");
        }

        @Override
        public List<RoomTransferRequest> getPendingTargetHolderApprovals(Long holderUserId) {
            throw unexpectedRepositoryCall("RoomTransferUseCase.getPendingTargetHolderApprovals");
        }
    }

    private static final class FailingInvoiceLineRepository implements InvoiceLineRepository {
        @Override
        public InvoiceLine save(InvoiceLine invoiceLine) {
            throw unexpectedRepositoryCall("InvoiceLineRepository.save");
        }

        @Override
        public Optional<InvoiceLine> findById(Long id) {
            throw unexpectedRepositoryCall("InvoiceLineRepository.findById");
        }

        @Override
        public Optional<InvoiceLine> findByInvoiceId(Long invoiceId) {
            throw unexpectedRepositoryCall("InvoiceLineRepository.findByInvoiceId");
        }
    }

    private static AssertionError unexpectedRepositoryCall(String methodName) {
        return new AssertionError(methodName + " should not be called for deposit invoice completion tests.");
    }

}
