package org.example.rfid.scenario.payment;

import org.example.rfid.model.PaymentEvent;
import org.example.rfid.scenario.contracts.PaymentEventPublisher;
import org.example.rfid.scenario.coordinator.ProductJourneyCoordinator;
import org.example.rfid.scenario.model.PendingPaymentNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    private static final int MAX_PUBLISH_ATTEMPTS_PER_TRANSACTION = 20;

    @Mock
    private ProductJourneyCoordinator coordinator;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @Test
    void constructorRejectsInvalidArguments() {
        assertEquals(
                "coordinator must not be null",
                assertThrows(NullPointerException.class, () -> new PaymentProcessor(null, eventPublisher, "CAJA-01")).getMessage()
        );
        assertEquals(
                "eventPublisher must not be null",
                assertThrows(NullPointerException.class, () -> new PaymentProcessor(coordinator, null, "CAJA-01")).getMessage()
        );
        assertEquals(
                "cashierReaderId must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new PaymentProcessor(coordinator, eventPublisher, "   ")).getMessage()
        );
    }

    @Test
    void tickRejectsNonPositiveMaxTransactions() {
        PaymentProcessor processor = new PaymentProcessor(coordinator, eventPublisher, "CAJA-01");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> processor.tick(0)
        );

        assertEquals("maxTransactionsPerTick must be > 0", exception.getMessage());
        verify(coordinator, never()).processPendingPayments(anyInt());
    }

    @Test
    void tickProcessesPendingPaymentsAndPublishesSuccessfulNotification() {
        PaymentProcessor processor = new PaymentProcessor(coordinator, eventPublisher, "CAJA-01");
        PendingPaymentNotification notification = new PendingPaymentNotification("tx-1", List.of("EPC-1", "EPC-2"));

        doReturn(List.of(notification)).when(coordinator).fetchPendingPaymentNotifications(3);
        when(eventPublisher.publish(any(PaymentEvent.class))).thenReturn(true);

        long before = System.currentTimeMillis();
        processor.tick(3);
        long after = System.currentTimeMillis();

        verify(coordinator).processPendingPayments(3);
        verify(coordinator).fetchPendingPaymentNotifications(3);
        verify(coordinator).markPaymentNotificationEmitted("tx-1");

        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        PaymentEvent event = eventCaptor.getValue();
        assertEquals("tx-1", event.transactionId());
        assertEquals("CAJA-01", event.cashierReaderId());
        assertEquals(List.of("EPC-1", "EPC-2"), event.paidEpcs());
        assertTrue(event.timestamp() >= before && event.timestamp() <= after);
    }

    @Test
    void tickRetriesFailedPublishAndMarksAsEmittedOnTwentiethFailure() {
        PaymentProcessor processor = new PaymentProcessor(coordinator, eventPublisher, "CAJA-01");
        PendingPaymentNotification notification = new PendingPaymentNotification("tx-2", List.of("EPC-9"));

        doReturn(List.of(notification)).when(coordinator).fetchPendingPaymentNotifications(1);
        when(eventPublisher.publish(any(PaymentEvent.class))).thenReturn(false);

        for (int i = 0; i < MAX_PUBLISH_ATTEMPTS_PER_TRANSACTION - 1; i++) {
            processor.tick(1);
        }

        verify(coordinator, never()).markPaymentNotificationEmitted("tx-2");

        processor.tick(1);
        verify(coordinator, times(1)).markPaymentNotificationEmitted("tx-2");

        processor.tick(1);
        verify(coordinator, times(1)).markPaymentNotificationEmitted("tx-2");
    }

    @Test
    void tickClearsFailureCounterAfterSuccess() {
        PaymentProcessor processor = new PaymentProcessor(coordinator, eventPublisher, "CAJA-01");
        PendingPaymentNotification notification = new PendingPaymentNotification("tx-3", List.of("EPC-7"));

        doReturn(List.of(notification)).when(coordinator).fetchPendingPaymentNotifications(1);

        AtomicInteger publishCalls = new AtomicInteger(0);
        when(eventPublisher.publish(any(PaymentEvent.class))).thenAnswer(invocation -> {
            int call = publishCalls.incrementAndGet();
            if (call <= 2) {
                return false;
            }
            if (call == 3) {
                return true;
            }
            return false;
        });

        processor.tick(1);
        processor.tick(1);
        processor.tick(1);

        verify(coordinator, times(1)).markPaymentNotificationEmitted("tx-3");

        for (int i = 0; i < MAX_PUBLISH_ATTEMPTS_PER_TRANSACTION; i++) {
            processor.tick(1);
        }

        verify(coordinator, times(2)).markPaymentNotificationEmitted("tx-3");
    }
}
