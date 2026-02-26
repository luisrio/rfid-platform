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
class FittingRoomScenarioTest {

    @Mock
    private ProductJourneyCoordinator coordinator;

    @Test
    void constructorRejectsInvalidArguments() {
        assertEquals(
                "readerId must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new FittingRoomScenario("   ", coordinator)).getMessage()
        );
        assertEquals(
                "coordinator must not be null",
                assertThrows(NullPointerException.class, () -> new FittingRoomScenario("PROBADOR-01", null)).getMessage()
        );
    }

    @Test
    void nextEventsReturnsEmptyWhenCoordinatorReturnsNoEpcs() {
        lenient().when(coordinator.nextForFitting(1)).thenReturn(List.of());
        lenient().when(coordinator.nextForFitting(2)).thenReturn(List.of());
        lenient().when(coordinator.nextForFitting(3)).thenReturn(List.of());

        FittingRoomScenario scenario = new FittingRoomScenario("PROBADOR-01", coordinator);
        List<RfidEvent> events = scenario.nextEvents();

        assertTrue(events.isEmpty());

        ArgumentCaptor<Integer> maxProductsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(coordinator).nextForFitting(maxProductsCaptor.capture());
        int requested = maxProductsCaptor.getValue();
        assertTrue(requested >= 1 && requested <= 3);
    }

    @Test
    void nextEventsProducesReadsWithinExpectedRanges() {
        List<String> epcs = List.of("EPC-X", "EPC-Y");
        lenient().when(coordinator.nextForFitting(1)).thenReturn(epcs);
        lenient().when(coordinator.nextForFitting(2)).thenReturn(epcs);
        lenient().when(coordinator.nextForFitting(3)).thenReturn(epcs);

        FittingRoomScenario scenario = new FittingRoomScenario("PROBADOR-01", coordinator);

        long before = System.currentTimeMillis();
        List<RfidEvent> events = scenario.nextEvents();
        long after = System.currentTimeMillis();

        assertTrue(events.size() >= 1 && events.size() <= 2);
        Set<String> allowedEpcs = Set.copyOf(epcs);
        for (RfidEvent event : events) {
            assertTrue(allowedEpcs.contains(event.epc()));
            assertEquals("PROBADOR-01", event.readerId());
            assertTrue(event.rssi() >= -77 && event.rssi() <= -60);
            assertTrue(event.timestamp() >= before && event.timestamp() <= after);
        }
    }
}
