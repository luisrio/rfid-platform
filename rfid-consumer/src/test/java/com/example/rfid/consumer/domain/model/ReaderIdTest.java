package com.example.rfid.consumer.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReaderIdTest {

    @Test
    void constructor_rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new ReaderId(null));
    }

    @Test
    void constructor_rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new ReaderId("  "));
    }

    @Test
    void constructor_acceptsNonBlank() {
        ReaderId readerId = new ReaderId("GATE-01");
        assertEquals("GATE-01", readerId.value());
    }
}
