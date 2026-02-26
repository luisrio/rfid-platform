package org.example.rfid.scenario.engine;

import org.example.rfid.scenario.model.ProductState;
import org.example.rfid.scenario.model.Stage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryEngineTest {

    @Test
    void nextForStageReturnsEmptyWhenMaxProductsIsNonPositive() {
        InventoryEngine engine = new InventoryEngine(
                1.0,
                1.0,
                0.0,
                10,
                new TestRandomSource(List.of(0.0), List.of(0))
        );

        List<String> selected = engine.nextForStage(Stage.CASHIER, 0, epcs -> {
        });

        assertTrue(selected.isEmpty());
        assertTrue(engine.products().isEmpty());
    }

    @Test
    void fittingReadMovesProductToExitWhenSkipCashierRateIsOne() {
        InventoryEngine engine = new InventoryEngine(
                0.0,
                0.0,
                1.0,
                1,
                new TestRandomSource(List.of(0.10), List.of(1))
        );
        engine.products().put("EPC-1", new ProductState("EPC-1", Stage.FITTING, 1));

        List<String> enqueued = new ArrayList<>();
        List<String> selected = engine.nextForStage(Stage.FITTING, 1, enqueued::addAll);

        assertEquals(List.of("EPC-1"), selected);
        assertTrue(enqueued.isEmpty());
        ProductState product = engine.products().get("EPC-1");
        assertEquals(Stage.EXIT, product.stage);
        assertEquals(2, product.remainingStageTicks);
    }

    @Test
    void fittingReadMovesProductToCashierWhenSkipCashierRateIsZero() {
        InventoryEngine engine = new InventoryEngine(
                0.0,
                0.0,
                0.0,
                1,
                new TestRandomSource(List.of(0.90), List.of(2))
        );
        engine.products().put("EPC-2", new ProductState("EPC-2", Stage.FITTING, 1));

        List<String> selected = engine.nextForStage(Stage.FITTING, 1, epcs -> {
        });

        assertEquals(List.of("EPC-2"), selected);
        ProductState product = engine.products().get("EPC-2");
        assertEquals(Stage.CASHIER, product.stage);
        assertEquals(3, product.remainingStageTicks);
    }

    @Test
    void cashierReadMovesProductToPaymentPendingAndEnqueuesIt() {
        InventoryEngine engine = new InventoryEngine(
                0.0,
                0.0,
                0.0,
                1,
                new TestRandomSource(List.of(), List.of())
        );
        engine.products().put("EPC-3", new ProductState("EPC-3", Stage.CASHIER, 1));

        List<String> enqueued = new ArrayList<>();
        List<String> selected = engine.nextForStage(Stage.CASHIER, 1, enqueued::addAll);

        assertEquals(List.of("EPC-3"), selected);
        assertEquals(List.of("EPC-3"), enqueued);
        ProductState product = engine.products().get("EPC-3");
        assertEquals(Stage.PAYMENT_PENDING, product.stage);
        assertEquals(0, product.remainingStageTicks);
    }

    @Test
    void exitReadRemovesProductFromInventory() {
        InventoryEngine engine = new InventoryEngine(
                0.0,
                0.0,
                0.0,
                1,
                new TestRandomSource(List.of(), List.of())
        );
        engine.products().put("EPC-4", new ProductState("EPC-4", Stage.EXIT, 1));

        List<String> selected = engine.nextForStage(Stage.EXIT, 1, epcs -> {
        });

        assertEquals(List.of("EPC-4"), selected);
        assertTrue(engine.products().isEmpty());
    }

    @Test
    void emptyInventorySpawnsProductUsingInjectedRandom() {
        InventoryEngine engine = new InventoryEngine(
                0.0,
                0.0,
                0.0,
                5,
                new TestRandomSource(List.of(0.95), List.of(0))
        );

        List<String> enqueued = new ArrayList<>();
        List<String> selected = engine.nextForStage(Stage.CASHIER, 1, enqueued::addAll);

        assertEquals(1, selected.size());
        assertEquals(selected, enqueued);
        String epc = selected.get(0);
        ProductState product = engine.products().get(epc);
        assertEquals(Stage.PAYMENT_PENDING, product.stage);
        assertEquals(0, product.remainingStageTicks);
    }
}
