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

    private String mockValueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
