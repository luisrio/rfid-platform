package org.example.rfid.scenario.engine;

import org.example.rfid.scenario.model.CheckoutCandidate;
import org.example.rfid.scenario.model.PendingPaymentNotification;
import org.example.rfid.scenario.model.ProductState;
import org.example.rfid.scenario.model.Stage;
import org.example.rfid.util.RandomIds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CheckoutEngine {

    private final ArrayDeque<CheckoutCandidate> pendingCheckouts = new ArrayDeque<>();
    private final Map<String, List<String>> awaitingNotificationByTx = new LinkedHashMap<>();
    private final double paymentTransactionRate;
    private final double paymentItemRate;
    private final double unpaidExitRate;
    private final double postCashierFittingRate;
    private final RandomSource random;

    public CheckoutEngine(
            double paymentTransactionRate,
            double paymentItemRate,
            double unpaidExitRate,
            double postCashierFittingRate
    ) {
        this(
                paymentTransactionRate,
                paymentItemRate,
                unpaidExitRate,
                postCashierFittingRate,
                RandomSource.threadLocal()
        );
    }

    public CheckoutEngine(
            double paymentTransactionRate,
            double paymentItemRate,
            double unpaidExitRate,
            double postCashierFittingRate,
            RandomSource random
    ) {
        this.paymentTransactionRate = paymentTransactionRate;
        this.paymentItemRate = paymentItemRate;
        this.unpaidExitRate = unpaidExitRate;
        this.postCashierFittingRate = postCashierFittingRate;
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    public void enqueueCheckoutIfReady(List<String> epcs) {
        if (epcs.isEmpty()) {
            return;
        }
        pendingCheckouts.addLast(new CheckoutCandidate(RandomIds.transactionId(), List.copyOf(epcs)));
    }

    public void processPendingPayments(int maxTransactions, Map<String, ProductState> products) {
        if (maxTransactions <= 0 || pendingCheckouts.isEmpty()) {
            return;
        }

        for (int i = 0; i < maxTransactions; i++) {
            CheckoutCandidate checkout = pendingCheckouts.pollFirst();
            if (checkout == null) {
                break;
            }
            processCheckout(checkout, random, products);
        }
    }

    public List<PendingPaymentNotification> fetchPendingPaymentNotifications(int maxNotifications) {
        if (maxNotifications <= 0 || awaitingNotificationByTx.isEmpty()) {
            return List.of();
        }

        List<PendingPaymentNotification> notifications = new ArrayList<>(
                Math.min(maxNotifications, awaitingNotificationByTx.size())
        );

        int added = 0;
        for (Map.Entry<String, List<String>> entry : awaitingNotificationByTx.entrySet()) {
            notifications.add(new PendingPaymentNotification(entry.getKey(), entry.getValue()));
            added++;
            if (added >= maxNotifications) {
                break;
            }
        }

        return notifications;
    }

    public void markPaymentNotificationEmitted(String transactionId, Map<String, ProductState> products) {
        List<String> paidEpcs = awaitingNotificationByTx.remove(transactionId);
        if (paidEpcs == null || paidEpcs.isEmpty()) {
            return;
        }

        for (String epc : paidEpcs) {
            ProductState product = products.get(epc);
            if (product == null || product.stage != Stage.PAID_AWAITING_NOTIFICATION) {
                continue;
            }

            if (random.nextDouble() < postCashierFittingRate) {
                product.stage = Stage.FITTING;
                product.remainingStageTicks = randomBetween(random, 2, 8);
            } else {
                product.stage = Stage.EXIT;
                product.remainingStageTicks = randomBetween(random, 1, 3);
            }
        }
    }

    private void processCheckout(
            CheckoutCandidate checkout,
            RandomSource random,
            Map<String, ProductState> products
    ) {
        boolean attempted = random.nextDouble() < paymentTransactionRate;
        List<String> paidEpcs = new ArrayList<>();
        List<String> toReturnStock = new ArrayList<>();

        for (String epc : checkout.epcs) {
            ProductState product = products.get(epc);
            if (product == null || product.stage != Stage.PAYMENT_PENDING) {
                continue;
            }
            applyCheckoutDecision(product, attempted, random, paidEpcs, toReturnStock);
        }

        for (String epc : toReturnStock) {
            products.remove(epc);
        }

        if (!paidEpcs.isEmpty()) {
            awaitingNotificationByTx.put(checkout.transactionId, List.copyOf(paidEpcs));
        }
    }

    private void applyCheckoutDecision(
            ProductState product,
            boolean attempted,
            RandomSource random,
            List<String> paidEpcs,
            List<String> toReturnStock
    ) {
        boolean paid = attempted && random.nextDouble() < paymentItemRate;
        if (paid) {
            product.stage = Stage.PAID_AWAITING_NOTIFICATION;
            product.remainingStageTicks = 0;
            paidEpcs.add(product.epc);
            return;
        }

        if (random.nextDouble() < unpaidExitRate) {
            product.stage = Stage.EXIT;
            product.remainingStageTicks = randomBetween(random, 1, 3);
            return;
        }

        toReturnStock.add(product.epc);
    }

    private static int randomBetween(RandomSource random, int minInclusive, int maxInclusive) {
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

}
