package com.sep490.hdbhms.billingandpayment.infrastructure.adapter;


import com.sep490.hdbhms.billingandpayment.application.port.out.ExternalPaymentPort;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.config.PayOSProperties;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.PaymentRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PayOSAdapter implements ExternalPaymentPort {
    PayOSProperties payOSProperties;

    @Override
    public PaymentIntent createCheckoutRequest(PaymentRequest request) {
        CreatePaymentLinkRequest createPaymentLinkRequest = CreatePaymentLinkRequest.builder()
                .orderCode(request.paymentId())
                .amount(request.amount())
                .description(request.description())
                .returnUrl(payOSProperties.getReturnUrl())
                .cancelUrl(payOSProperties.getCancelUrl())
                .build();
        try {
            CreatePaymentLinkResponse createPaymentLinkResponse = payOSProperties.payOS()
                    .paymentRequests()
                    .create(createPaymentLinkRequest);
            return new PaymentIntent(
                    request.paymentId(),
                    createPaymentLinkResponse.getCheckoutUrl(),
                    PaymentIntentProvider.BANK_TRANSFER,
                    PaymentStatus.PENDING
            );
        } catch (PayOSException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public PaymentStatus checkPaymentStatus(String paymentId) {
        return null;
    }
}
