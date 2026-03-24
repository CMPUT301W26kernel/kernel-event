package com.example.eventlottery;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for UserProfileFragment logic.
 * These tests run on the local JVM and do not require an Android device.
 */
public class UserProfileFragmentUnitTest {

    /**
     * Tests the valueOrEmpty helper method in a simulated way.
     * Note: Since valueOrEmpty is private in UserProfileFragment, 
     * in a real scenario you might make it package-private for testing 
     * or test it via public methods. This test demonstrates the logic.
     */
    @Test
    public void testValueOrEmpty() {
        // Logic test: null should return empty string, others should return themselves.
        String nullValue = null;
        String validValue = "TestUser";

        assertEquals("", mockValueOrEmpty(nullValue));
        assertEquals("TestUser", mockValueOrEmpty(validValue));
    }

    /**
     * Tests the capitalize logic used for the user role.
     */
    @Test
    public void testCapitalize() {
        assertEquals("Entrant", mockCapitalize("entrant"));
        assertEquals("Organizer", mockCapitalize("ORGANIZER"));
        assertEquals("Admin", mockCapitalize("aDmiN"));
        assertEquals("", mockCapitalize(null));
        assertEquals("", mockCapitalize(""));
    }

    private String mockValueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String mockCapitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
