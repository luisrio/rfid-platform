package org.example.rfid.scenario.engine;

import org.example.rfid.scenario.model.PendingPaymentNotification;
import org.example.rfid.scenario.model.ProductState;
import org.example.rfid.scenario.model.Stage;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckoutEngineTest {

    @Test
    void enqueueCheckoutIfReadyIgnoresEmptyInput() {
        CheckoutEngine engine = new CheckoutEngine(
                1.0,
                1.0,
                0.0,
                0.0,
                new TestRandomSource(List.of(), List.of())
        );

        engine.enqueueCheckoutIfReady(List.of());

        assertTrue(engine.fetchPendingPaymentNotifications(10).isEmpty());
    }

    @Test
    void successfulCheckoutCreatesPendingNotificationAndMovesToPaidAwaitingNotification() {
        CheckoutEngine engine = new CheckoutEngine(
                1.0,
                1.0,
                0.0,
                0.0,
                new TestRandomSource(List.of(0.1, 0.1, 0.7), List.of(2))
        );
        Map<String, ProductState> products = new LinkedHashMap<>();
        products.put("EPC-1", new ProductState("EPC-1", Stage.PAYMENT_PENDING, 0));

        engine.enqueueCheckoutIfReady(List.of("EPC-1"));
        engine.processPendingPayments(1, products);

        ProductState product = products.get("EPC-1");
        assertEquals(Stage.PAID_AWAITING_NOTIFICATION, product.stage);
        assertEquals(0, product.remainingStageTicks);

        List<PendingPaymentNotification> notifications = engine.fetchPendingPaymentNotifications(10);
        assertEquals(1, notifications.size());
        assertEquals(List.of("EPC-1"), notifications.get(0).paidEpcs());

        String transactionId = notifications.get(0).transactionId();
        engine.markPaymentNotificationEmitted(transactionId, products);

        assertTrue(engine.fetchPendingPaymentNotifications(10).isEmpty());
        assertEquals(Stage.EXIT, product.stage);
        assertEquals(3, product.remainingStageTicks);
    }

    @Test
    void unpaidItemReturnsToStockWhenUnpaidExitRateIsZero() {
        CheckoutEngine engine = new CheckoutEngine(
                1.0,
                0.0,
                0.0,
                0.0,
                new TestRandomSource(List.of(0.1, 0.8, 0.9), List.of())
        );
        Map<String, ProductState> products = new LinkedHashMap<>();
        products.put("EPC-2", new ProductState("EPC-2", Stage.PAYMENT_PENDING, 0));

        engine.enqueueCheckoutIfReady(List.of("EPC-2"));
        engine.processPendingPayments(1, products);

        assertTrue(products.isEmpty());
        assertTrue(engine.fetchPendingPaymentNotifications(10).isEmpty());
    }

    @Test
    void unpaidItemMovesToExitWhenUnpaidExitRateIsOne() {
        CheckoutEngine engine = new CheckoutEngine(
                1.0,
                0.0,
                1.0,
                0.0,
                new TestRandomSource(List.of(0.1, 0.8, 0.5), List.of(0))
        );
        Map<String, ProductState> products = new LinkedHashMap<>();
        products.put("EPC-3", new ProductState("EPC-3", Stage.PAYMENT_PENDING, 0));

        engine.enqueueCheckoutIfReady(List.of("EPC-3"));
        engine.processPendingPayments(1, products);

        ProductState product = products.get("EPC-3");
        assertEquals(Stage.EXIT, product.stage);
        assertEquals(1, product.remainingStageTicks);
        assertTrue(engine.fetchPendingPaymentNotifications(10).isEmpty());
    }

    @Test
    void markPaymentNotificationEmittedCanMoveBackToFittingRoom() {
        CheckoutEngine engine = new CheckoutEngine(
                1.0,
                1.0,
                0.0,
                1.0,
                new TestRandomSource(List.of(0.1, 0.1, 0.1), List.of(4))
        );
        Map<String, ProductState> products = new LinkedHashMap<>();
        products.put("EPC-4", new ProductState("EPC-4", Stage.PAYMENT_PENDING, 0));

        engine.enqueueCheckoutIfReady(List.of("EPC-4"));
        engine.processPendingPayments(1, products);

        PendingPaymentNotification notification = engine.fetchPendingPaymentNotifications(1).get(0);
        engine.markPaymentNotificationEmitted(notification.transactionId(), products);

        ProductState product = products.get("EPC-4");
        assertEquals(Stage.FITTING, product.stage);
        assertEquals(6, product.remainingStageTicks);
    }

    @Test
    void fetchPendingPaymentNotificationsRespectsMaxLimit() {
        CheckoutEngine engine = new CheckoutEngine(
                1.0,
                1.0,
                0.0,
                0.0,
                new TestRandomSource(List.of(0.1, 0.1, 0.1, 0.1), List.of())
        );
        Map<String, ProductState> products = new LinkedHashMap<>();
        products.put("EPC-5", new ProductState("EPC-5", Stage.PAYMENT_PENDING, 0));
        products.put("EPC-6", new ProductState("EPC-6", Stage.PAYMENT_PENDING, 0));

        engine.enqueueCheckoutIfReady(List.of("EPC-5"));
        engine.enqueueCheckoutIfReady(List.of("EPC-6"));
        engine.processPendingPayments(2, products);

        List<PendingPaymentNotification> limited = engine.fetchPendingPaymentNotifications(1);
        assertEquals(1, limited.size());
    }
}
