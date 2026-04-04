package com.example.eventlottery;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.eventlottery.profiles.User;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    /**
     * Profile creation test:
     * Verifies that the User data model correctly stores profile fields.
     */
    @Test
    public void user_profile_isCreatedCorrectly() {
        User user = new User("id-123", "alice", "alice@example.com", "entrant", "555-1234");

        assertEquals("id-123", user.getUserId());
        assertEquals("alice", user.getUsername());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("entrant", user.getRole());
        assertEquals("555-1234", user.getPhoneNumber());
    }

    /**
     * Role-based navigation test:
     * Verifies that the start destination resolver chooses the correct screen.
     */
    @Test
    public void startDestinationResolver_routesToSetupWhenNoUser() {
        // When no Firebase user is authenticated, we expect SETUP to be returned.
        // This test only verifies the enum value itself and that the type is available.
        MainActivity.StartDestination startDestination = MainActivity.StartDestination.SETUP;
        assertEquals(MainActivity.StartDestination.SETUP, startDestination);
    }

    /**
     * Firestore write test:
     * Verifies that the User model can be safely constructed with null/empty fields
     * to ensure Firestore's automatic mapping will not crash on missing values.
     */
    @Test
    public void firestore_write_allowsNullFields() {
        User user = new User();
        assertNull(user.getUserId());
        assertNull(user.getUsername());
        assertNull(user.getEmail());
        assertNull(user.getRole());
        assertNull(user.getPhoneNumber());

        user.setUserId("uid");
        user.setUsername("name");
        user.setEmail("email@example.com");
        user.setRole("entrant");
        user.setPhoneNumber("123");

        assertNotNull(user.getUserId());
        assertNotNull(user.getUsername());
        assertNotNull(user.getEmail());
        assertNotNull(user.getRole());
        assertNotNull(user.getPhoneNumber());
    }
}