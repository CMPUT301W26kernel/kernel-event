package com.example.eventlottery;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for UserProfileFragment logic.
 * These tests run on the local JVM and do not require an Android device.
 */
public class UserProfileFragmentUnitTest {

    private UserProfileFragment fragment;

    @Before
    public void setUp() {
        fragment = new UserProfileFragment();
    }

    /**
     * Tests the valueOrEmpty helper method.
     */
    @Test
    public void testValueOrEmpty() {
        String nullValue = null;
        String validValue = "TestUser";

        assertEquals("", fragment.valueOrEmpty(nullValue));
        assertEquals("TestUser", fragment.valueOrEmpty(validValue));
    }

    /**
     * Tests the capitalize logic used for the user role.
     */
    @Test
    public void testCapitalize() {
        assertEquals("Entrant", fragment.capitalize("entrant"));
        assertEquals("Organizer", fragment.capitalize("ORGANIZER"));
        assertEquals("Admin", fragment.capitalize("aDmiN"));
        assertEquals("", fragment.capitalize(null));
        assertEquals("", fragment.capitalize(""));
    }
}
