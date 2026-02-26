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
class ExitGateScenarioTest {

    @Mock
    private ProductJourneyCoordinator coordinator;

    @Test
    void constructorRejectsInvalidArguments() {
        assertEquals(
                "readerId must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new ExitGateScenario("   ", coordinator)).getMessage()
        );
        assertEquals(
                "coordinator must not be null",
                assertThrows(NullPointerException.class, () -> new ExitGateScenario("GATE-01", null)).getMessage()
        );
    }

    @Test
    void nextEventsReturnsEmptyWhenCoordinatorReturnsNoEpcs() {
        lenient().when(coordinator.nextForExit(1)).thenReturn(List.of());
        lenient().when(coordinator.nextForExit(2)).thenReturn(List.of());
        lenient().when(coordinator.nextForExit(3)).thenReturn(List.of());

        ExitGateScenario scenario = new ExitGateScenario("GATE-01", coordinator);
        List<RfidEvent> events = scenario.nextEvents();

        assertTrue(events.isEmpty());

        ArgumentCaptor<Integer> maxProductsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(coordinator).nextForExit(maxProductsCaptor.capture());
        int requested = maxProductsCaptor.getValue();
        assertTrue(requested >= 1 && requested <= 3);
    }

    @Test
    void nextEventsProducesGateReadsWithinExpectedRanges() {
        List<String> gateEpcs = List.of("EPC-EXIT-1", "EPC-EXIT-2");
        lenient().when(coordinator.nextForExit(1)).thenReturn(gateEpcs);
        lenient().when(coordinator.nextForExit(2)).thenReturn(gateEpcs);
        lenient().when(coordinator.nextForExit(3)).thenReturn(gateEpcs);

        ExitGateScenario scenario = new ExitGateScenario("GATE-01", coordinator);

        long before = System.currentTimeMillis();
        List<RfidEvent> events = scenario.nextEvents();
        long after = System.currentTimeMillis();

        assertTrue(events.size() >= gateEpcs.size());
        assertTrue(events.size() <= gateEpcs.size() * 4);

        Set<String> allowedEpcs = Set.copyOf(gateEpcs);
        for (RfidEvent event : events) {
            assertTrue(allowedEpcs.contains(event.epc()));
            assertEquals("GATE-01", event.readerId());
            assertTrue(event.rssi() >= -68 && event.rssi() <= -38);
            assertTrue(event.timestamp() >= before && event.timestamp() <= after);
        }
    }
}
