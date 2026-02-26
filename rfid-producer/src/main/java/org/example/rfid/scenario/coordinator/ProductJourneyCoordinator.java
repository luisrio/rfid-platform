package org.example.rfid.scenario.coordinator;


import org.example.rfid.scenario.engine.CheckoutEngine;
import org.example.rfid.scenario.engine.InventoryEngine;
import org.example.rfid.scenario.model.PendingPaymentNotification;
import org.example.rfid.scenario.model.Stage;

import java.util.List;

public final class ProductJourneyCoordinator {

    private final InventoryEngine inventory;
    private final CheckoutEngine checkout;

    public ProductJourneyCoordinator(
            double newProductRate,
            double fittingVisitRate,
            double postCashierFittingRate,
            double skipCashierRate,
            double paymentTransactionRate,
            double paymentItemRate,
            double unpaidExitRate,
            int maxActiveProducts
    ) {
        this.inventory = new InventoryEngine(
                newProductRate,
                fittingVisitRate,
                skipCashierRate,
                maxActiveProducts
        );
        this.checkout = new CheckoutEngine(
                paymentTransactionRate,
                paymentItemRate,
                unpaidExitRate,
                postCashierFittingRate
        );
    }

    public synchronized List<String> nextForFitting(int maxProducts) {
        return inventory.nextForStage(Stage.FITTING, maxProducts, checkout::enqueueCheckoutIfReady);
    }

    public synchronized List<String> nextForCashier(int maxProducts) {
        return inventory.nextForStage(Stage.CASHIER, maxProducts, checkout::enqueueCheckoutIfReady);
    }

    public synchronized List<String> nextForExit(int maxProducts) {
        return inventory.nextForStage(Stage.EXIT, maxProducts, checkout::enqueueCheckoutIfReady);
    }

    public synchronized void processPendingPayments(int maxTransactions) {
        checkout.processPendingPayments(maxTransactions, inventory.products());
    }

    public synchronized List<PendingPaymentNotification> fetchPendingPaymentNotifications(int maxNotifications) {
        return checkout.fetchPendingPaymentNotifications(maxNotifications);
    }

    public synchronized void markPaymentNotificationEmitted(String transactionId) {
        checkout.markPaymentNotificationEmitted(transactionId, inventory.products());
    }
}
