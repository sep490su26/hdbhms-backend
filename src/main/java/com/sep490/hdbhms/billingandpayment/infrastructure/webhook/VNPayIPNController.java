package com.sep490.hdbhms.billingandpayment.infrastructure.webhook;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;
import com.sep490.hdbhms.occupancy.application.port.in.command.ConfirmDepositPaymentCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ApproveDepositFormUseCase;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhook/vnpay")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VNPayIPNController {
    VNPaySignatureVerifier vnPaySignatureVerifier;
    ApproveDepositFormUseCase approveDepositFormUseCase;

    @GetMapping("/ipn")
    public Map<String, String> ipn(@RequestParam Map<String, String> params) {
        if (!vnPaySignatureVerifier.isValid(params)) {
            return Map.of("RspCode", "97", "Message", "Invalid signature");
        }

        String responseCode = params.get("vnp_ResponseCode");
        Long paymentIntentId = null;
        try {
            String txnRef = params.get("vnp_TxnRef");
            paymentIntentId = Long.parseLong(txnRef);
        } catch (NumberFormatException e) {
            return Map.of("RspCode", "97", "Message", "Invalid transaction reference");
        }
        log.info(String.valueOf(paymentIntentId));
        String transactionNo = params.get("vnp_TransactionNo"); // VNPay transaction number

        var status = "00".equals(responseCode) ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;

        approveDepositFormUseCase.confirmPayment(new ConfirmDepositPaymentCommand(paymentIntentId, status));

        return Map.of("RspCode", "00", "Message", "Confirm Success");
    }
}
