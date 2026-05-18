package com.sep490.hdbhms.billingandpayment.application.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BuyCoinsService {
//    PaymentRepository paymentRepository;
//
//    BalanceRepository balanceRepository;
//    TransactionRepository transactionRepository;
//
//    ExternalPaymentPort externalPaymentPort;
//
//    @Override
//    public PaymentIntent initiate(BuyCoinsCommand command) {
//        var domain = Payment.newPayment(command.accountId(), command.amount());
//        domain = paymentRepository.save(domain);
//
//        var request = new PaymentRequest(
//                domain.getId(),
//                domain.getAmount(),
//                command.returnUrl()
//        );
//        var intent = externalPaymentPort.createPayment(request);
//        domain.setProvider(intent.paymentProvider());
//        paymentRepository.save(domain);
//        return intent;
//    }
//
//    @Override
//    @Transactional
//    public void confirmation(String paymentId, PaymentStatus status) {
//        var payment = paymentRepository.findById(paymentId)
//                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
//
//        if (payment.getStatus() != PaymentStatus.PENDING) {
//            return;
//        }
//
//        if (status != PaymentStatus.SUCCEEDED) {
//            payment.markFailed();
//            paymentRepository.save(payment);
//            return;
//        }
//        var balance = balanceRepository.findById(payment.getAccountId())
//                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
//        balance.addCoins(payment.getAmount());
//
//        var transaction = Transaction.newTransaction(
//                payment.getAccountId(),
//                payment.getAmount(),
//                balance.getCoinBalance() + payment.getAmount(),
//                TransactionType.DEPOSIT,
//                String.format("Bought %d coins", payment.getAmount()),
//                null,
//                null
//        );
//        transaction = transactionRepository.save(transaction);
//        log.info(balance.toString());
//        balance.addTransaction(transaction);
//        balanceRepository.save(balance);
//
//        payment.markCompleted();
//        paymentRepository.save(payment);
//    }
}
