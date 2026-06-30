package com.sep490.hdbhms.billingandpayment.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.application.port.out.ExternalPaymentPort;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.PaymentRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IssuedInvoiceChargeServiceTest {

    @Test
    void issueMaintenanceChargePublishesInvoiceAndCreatesPayOsCheckout() {
        AtomicReference<PaymentRequest> capturedRequest = new AtomicReference<>();
        PaymentIntent checkout = new PaymentIntent(
                99L,
                "https://pay.payos.vn/checkout",
                PaymentIntentProvider.PAYOS,
                PaymentStatus.PENDING,
                200_000L,
                "665117859709184",
                "INV-MNT-1",
                "qr-code",
                "qr-payload",
                null,
                665117859709184L,
                "payment-link-id",
                "970418",
                "BIDV",
                "1510905318",
                "HAI DANG",
                "INV MNT 1"
        );

        ExternalPaymentPort externalPaymentPort = proxy(ExternalPaymentPort.class, (method, arguments) -> {
            if ("createCheckoutRequest".equals(method.getName())) {
                capturedRequest.set((PaymentRequest) arguments[0]);
                return checkout;
            }
            return null;
        });
        Environment environment = proxy(Environment.class, (method, arguments) -> {
            if ("getProperty".equals(method.getName()) && arguments.length >= 1
                    && "app.payment.provider".equals(arguments[0])) {
                return "payos";
            }
            return defaultValue(method.getReturnType());
        });

        IssuedInvoiceChargeService service = new IssuedInvoiceChargeService(
                passthroughRepository(JpaInvoiceRepository.class),
                passthroughRepository(JpaInvoiceLineRepository.class),
                passthroughRepository(JpaPaymentIntentRepository.class),
                externalPaymentPort,
                environment,
                new ObjectMapper()
        );
        PropertyEntity property = PropertyEntity.builder().id(1L).name("Hai Dang").build();
        RoomEntity room = RoomEntity.builder().id(103L).property(property).roomCode("103").name("Phong 103").build();
        LeaseContractEntity contract = LeaseContractEntity.builder().id(1L).room(room).contractCode("HD-103").build();

        IssuedInvoiceChargeService.IssuedChargeResult result = service.issueMaintenanceCharge(
                room,
                contract,
                InvoiceLineType.MAINTENANCE_COMPENSATION,
                "Chi phi sua chua",
                200_000L,
                1L,
                UserEntity.builder().id(10L).build()
        );

        assertEquals(InvoiceStatus.ISSUED, result.invoice().getStatus());
        assertNotNull(result.invoice().getIssuedAt());
        assertEquals(PaymentIntentStatus.PENDING, result.paymentIntent().getStatus());
        assertEquals(PaymentIntentProvider.PAYOS, result.paymentIntent().getProvider());
        assertEquals("665117859709184", result.paymentIntent().getProviderOrderCode());
        assertNotNull(capturedRequest.get());
        assertEquals(200_000L, capturedRequest.get().amount());
    }

    private static <T> T passthroughRepository(Class<T> type) {
        return proxy(type, (method, arguments) -> {
            if ("save".equals(method.getName()) && arguments != null && arguments.length == 1) {
                return arguments[0];
            }
            return defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (instance, method, arguments) -> invocation.invoke(method, arguments == null ? new Object[0] : arguments)
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        if (Optional.class.isAssignableFrom(type)) return Optional.empty();
        if (List.class.isAssignableFrom(type)) return List.of();
        return null;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(java.lang.reflect.Method method, Object[] arguments) throws Throwable;
    }
}
