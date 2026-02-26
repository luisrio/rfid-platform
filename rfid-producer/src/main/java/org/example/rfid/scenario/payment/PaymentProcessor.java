package org.example.rfid.scenario.payment;

import org.example.rfid.model.PaymentEvent;
import org.example.rfid.scenario.contracts.PaymentEventPublisher;
import org.example.rfid.scenario.coordinator.ProductJourneyCoordinator;
import org.example.rfid.scenario.model.PendingPaymentNotification;
import org.example.rfid.util.Time;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PaymentProcessor {

    private static final int MAX_PUBLISH_ATTEMPTS_PER_TRANSACTION = 20;

    private final ProductJourneyCoordinator coordinator;
    private final PaymentEventPublisher eventPublisher;
    private final String cashierReaderId;
    private final Map<String, Integer> failedPublishAttemptsByTransactionId = new HashMap<>();

    public PaymentProcessor(
            ProductJourneyCoordinator coordinator,
            PaymentEventPublisher eventPublisher,
            String cashierReaderId
    ) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.cashierReaderId = requireNonBlank(cashierReaderId, "cashierReaderId must not be blank");
    }

    public void tick(int maxTransactionsPerTick) {
        if (maxTransactionsPerTick <= 0) {
            throw new IllegalArgumentException("maxTransactionsPerTick must be > 0");
        }

        coordinator.processPendingPayments(maxTransactionsPerTick);
        List<PendingPaymentNotification> notificationsToPublish =
                coordinator.fetchPendingPaymentNotifications(maxTransactionsPerTick);

        for (PendingPaymentNotification notification : notificationsToPublish) {
            String transactionId = notification.transactionId();
            boolean published = eventPublisher.publish(buildPaymentEvent(notification));
            if (published) {
                failedPublishAttemptsByTransactionId.remove(transactionId);
                coordinator.markPaymentNotificationEmitted(transactionId);
                continue;
            }

            int attempts = failedPublishAttemptsByTransactionId.merge(transactionId, 1, Integer::sum);
            if (attempts >= MAX_PUBLISH_ATTEMPTS_PER_TRANSACTION) {
                failedPublishAttemptsByTransactionId.remove(transactionId);
                // Give up after N failures so the simulator does not retry forever.
                coordinator.markPaymentNotificationEmitted(transactionId);
            }
        }
    }

    private PaymentEvent buildPaymentEvent(PendingPaymentNotification notification) {
        return new PaymentEvent(
                notification.transactionId(),
                cashierReaderId,
                Time.nowMillis(),
                notification.paidEpcs()
        );
    }

    private static String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
