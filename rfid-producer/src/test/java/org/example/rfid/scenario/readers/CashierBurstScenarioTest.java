package org.example.rfid.scenario.readers;

import org.example.rfid.model.RfidEvent;
import org.example.rfid.scenario.coordinator.ProductJourneyCoordinator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CashierBurstScenarioTest {

    @Mock
    private ProductJourneyCoordinator coordinator;

    @Test
    void constructorRejectsInvalidArguments() {
        assertEquals(
                "readerId must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new CashierBurstScenario("   ", 1, coordinator)).getMessage()
        );
        assertEquals(
                "maxDistinctEpcsPerTick must be > 0",
                assertThrows(IllegalArgumentException.class, () -> new CashierBurstScenario("CAJA-01", 0, coordinator)).getMessage()
        );
        assertEquals(
                "coordinator must not be null",
                assertThrows(NullPointerException.class, () -> new CashierBurstScenario("CAJA-01", 1, null)).getMessage()
        );
    }

    @Test
    void nextEventsReturnsEmptyWhenCoordinatorReturnsNoEpcs() {
        lenient().when(coordinator.nextForCashier(1)).thenReturn(List.of());
        lenient().when(coordinator.nextForCashier(2)).thenReturn(List.of());
        lenient().when(coordinator.nextForCashier(3)).thenReturn(List.of());

        CashierBurstScenario scenario = new CashierBurstScenario("CAJA-01", 3, coordinator);
        List<RfidEvent> events = scenario.nextEvents();

        assertTrue(events.isEmpty());

        ArgumentCaptor<Integer> maxProductsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(coordinator).nextForCashier(maxProductsCaptor.capture());
        int requested = maxProductsCaptor.getValue();
        assertTrue(requested >= 1 && requested <= 3);
    }

    @Test
    void nextEventsProducesBurstWithExpectedFieldRanges() {
        List<String> basket = List.of("EPC-A", "EPC-B", "EPC-C");
        lenient().when(coordinator.nextForCashier(1)).thenReturn(basket);
        lenient().when(coordinator.nextForCashier(2)).thenReturn(basket);
        lenient().when(coordinator.nextForCashier(3)).thenReturn(basket);

        CashierBurstScenario scenario = new CashierBurstScenario("CAJA-01", 3, coordinator);

        long before = System.currentTimeMillis();
        List<RfidEvent> events = scenario.nextEvents();
        long after = System.currentTimeMillis();

        assertTrue(events.size() >= 6 && events.size() <= 18);
        Set<String> allowedEpcs = Set.copyOf(basket);
        for (RfidEvent event : events) {
            assertTrue(allowedEpcs.contains(event.epc()));
            assertEquals("CAJA-01", event.readerId());
            assertTrue(event.rssi() >= -59 && event.rssi() <= -45);
            assertTrue(event.timestamp() >= before && event.timestamp() <= after);
        }
    }
}
