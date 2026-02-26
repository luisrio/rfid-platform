package com.example.rfid.consumer.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EpcTest {

    @Test
    void constructor_rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Epc(null));
    }

    @Test
    void constructor_rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Epc("   "));
    }

    @Test
    void constructor_acceptsNonBlank() {
        Epc epc = new Epc("EPC-13B4368C666B");
        assertEquals("EPC-13B4368C666B", epc.value());
    }
}
