package org.example.rfid.scenario.engine;

import org.example.rfid.scenario.model.ProductState;
import org.example.rfid.scenario.model.Stage;
import org.example.rfid.scenario.model.StageReadResult;
import org.example.rfid.util.RandomIds;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class InventoryEngine {

    private static final double SECONDARY_SPAWN_FACTOR = 0.35;
    private static final double TERTIARY_SPAWN_FACTOR = 0.15;

    private final Map<String, ProductState> products = new LinkedHashMap<>();
    private final double newProductRate;
    private final double fittingVisitRate;
    private final double skipCashierRate;
    private final int maxActiveProducts;
    private final RandomSource random;

    public InventoryEngine(
            double newProductRate,
            double fittingVisitRate,
            double skipCashierRate,
            int maxActiveProducts
    ) {
        this(newProductRate, fittingVisitRate, skipCashierRate, maxActiveProducts, RandomSource.threadLocal());
    }

    InventoryEngine(
            double newProductRate,
            double fittingVisitRate,
            double skipCashierRate,
            int maxActiveProducts,
            RandomSource random
    ) {
        this.newProductRate = newProductRate;
        this.fittingVisitRate = fittingVisitRate;
        this.skipCashierRate = skipCashierRate;
        this.maxActiveProducts = maxActiveProducts;
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    public List<String> nextForStage(Stage stage, int maxProducts, Consumer<List<String>> checkoutEnqueuer) {
        if (maxProducts <= 0) {
            return List.of();
        }

        spawnNewProducts();

        List<ProductState> candidates = collectCandidatesForStage(stage);
        if (candidates.isEmpty()) {
            return List.of();
        }

        shuffle(candidates, random);

        int take = Math.min(maxProducts, candidates.size());
        StageReadResult readResult = processStageReads(candidates, take, random);

        checkoutEnqueuer.accept(readResult.toPaymentPending());
        removeProductsFromInventory(readResult.toRemove());

        return readResult.selectedEpcs();
    }

    public Map<String, ProductState> products() {
        return products;
    }

    private List<ProductState> collectCandidatesForStage(Stage stage) {
        List<ProductState> candidates = new ArrayList<>();
        for (ProductState product : products.values()) {
            if (product.stage == stage) {
                candidates.add(product);
            }
        }
        return candidates;
    }

    private StageReadResult processStageReads(List<ProductState> candidates, int take, RandomSource random) {
        List<String> selectedEpcs = new ArrayList<>(take);
        List<String> toRemove = new ArrayList<>();
        List<String> toPaymentPending = new ArrayList<>();

        for (int i = 0; i < take; i++) {
            ProductState product = candidates.get(i);
            selectedEpcs.add(product.epc);
            advanceProductAfterRead(product, random, toPaymentPending, toRemove);
        }

        return new StageReadResult(selectedEpcs, toPaymentPending, toRemove);
    }

    private void advanceProductAfterRead(
            ProductState product,
            RandomSource random,
            List<String> toPaymentPending,
            List<String> toRemove
    ) {
        product.remainingStageTicks--;
        if (product.remainingStageTicks > 0) {
            return;
        }

        switch (product.stage) {
            case FITTING -> {
                if (random.nextDouble() < skipCashierRate) {
                    product.stage = Stage.EXIT;
                    product.remainingStageTicks = randomBetween(random, 1, 3);
                } else {
                    product.stage = Stage.CASHIER;
                    product.remainingStageTicks = randomBetween(random, 1, 4);
                }
            }
            case CASHIER -> {
                product.stage = Stage.PAYMENT_PENDING;
                product.remainingStageTicks = 0;
                toPaymentPending.add(product.epc);
            }
            case PAYMENT_PENDING -> {
                // Payment scenario controls the next stage.
            }
            case PAID_AWAITING_NOTIFICATION -> {
                // Blocked until payment notification is published.
            }
            case EXIT -> toRemove.add(product.epc);
        }
    }

    private void removeProductsFromInventory(List<String> epcs) {
        for (String epc : epcs) {
            products.remove(epc);
        }
    }

    private void spawnNewProducts() {
        if (products.size() >= maxActiveProducts) {
            return;
        }

        if (products.isEmpty()) {
            createProduct(random);
            return;
        }

        int creations = computeCreations(random);
        createProducts(random, creations);
    }

    private int computeCreations(RandomSource random) {
        int creations = 0;
        if (random.nextDouble() < newProductRate) {
            creations++;
        }
        if (random.nextDouble() < newProductRate * SECONDARY_SPAWN_FACTOR) {
            creations++;
        }
        if (random.nextDouble() < newProductRate * TERTIARY_SPAWN_FACTOR) {
            creations++;
        }
        return creations;
    }

    private void createProducts(RandomSource random, int creations) {
        for (int i = 0; i < creations && products.size() < maxActiveProducts; i++) {
            createProduct(random);
        }
    }

    private void createProduct(RandomSource random) {
        String epc = nextUniqueEpc();

        boolean goFittingFirst = random.nextDouble() < fittingVisitRate;
        Stage stage = goFittingFirst ? Stage.FITTING : Stage.CASHIER;
        int stageTicks = goFittingFirst ? randomBetween(random, 3, 12) : randomBetween(random, 1, 5);

        products.put(epc, new ProductState(epc, stage, stageTicks));
    }

    private String nextUniqueEpc() {
        String epc = RandomIds.epc();
        while (products.containsKey(epc)) {
            epc = RandomIds.epc();
        }
        return epc;
    }

    private static int randomBetween(RandomSource random, int minInclusive, int maxInclusive) {
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    private static <T> void shuffle(List<T> values, RandomSource random) {
        for (int i = values.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T tmp = values.get(i);
            values.set(i, values.get(j));
            values.set(j, tmp);
        }
    }
}
