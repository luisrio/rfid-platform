package org.example.rfid.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileTest {

    @Test
    void fromArgDefaultsToMediumWhenNullOrBlank() {
        assertEquals(Profile.MEDIUM, Profile.fromArg(null));
        assertEquals(Profile.MEDIUM, Profile.fromArg(""));
        assertEquals(Profile.MEDIUM, Profile.fromArg("   "));
    }

    @Test
    void fromArgIsCaseInsensitiveForKnownProfiles() {
        assertEquals(Profile.LOW, Profile.fromArg("low"));
        assertEquals(Profile.MEDIUM, Profile.fromArg("MeDiuM"));
        assertEquals(Profile.HIGH, Profile.fromArg("HIGH"));
    }

    @Test
    void fromArgRejectsUnknownProfile() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Profile.fromArg("extreme")
        );

        assertTrue(exception.getMessage().contains("--profile must be one of"));
    }
}
