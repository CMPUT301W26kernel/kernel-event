package com.example.eventlottery;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests to verify the Co-Organizer logic works correctly.
 * These tests ensure the Event model properly stores co-organizers, and that the 
 * EventCommentPolicy grants them the proper elevated permissions.
 */
public class CoOrganizerFeatureTest {

    @Test
    public void testEventModelStoresCoOrganizers() {
        Event event = new Event();
        
        List<String> pending = new ArrayList<>();
        pending.add("entrant-pending-1");
        event.setPendingCoOrganizers(pending);

        List<String> accepted = new ArrayList<>();
        accepted.add("entrant-accepted-2");
        event.setCoOrganizers(accepted);

        assertEquals(1, event.getPendingCoOrganizers().size());
        assertEquals("entrant-pending-1", event.getPendingCoOrganizers().get(0));

        assertEquals(1, event.getCoOrganizers().size());
        assertEquals("entrant-accepted-2", event.getCoOrganizers().get(0));
    }

    @Test
    public void testCoOrganizerCanPostCommentAsOrganizer() {
        List<String> coOrganizers = new ArrayList<>();
        coOrganizers.add("co-organizer-user");

        // The Co-Organizer is testing trying to post.
        // Assuming their role displays as 'organizer' on this event.
        assertTrue(EventCommentPolicy.canPostComment("co-organizer-user", "organizer", "main-organizer", coOrganizers));
        
        // Ensure a random user who isn't the organizer or co-organizer but acting as "organizer" fails
        assertFalse(EventCommentPolicy.canPostComment("random-user", "organizer", "main-organizer", coOrganizers));

        // The Main organizer still can
        assertTrue(EventCommentPolicy.canPostComment("main-organizer", "organizer", "main-organizer", coOrganizers));
    }

    @Test
    public void testCoOrganizerCanPinComments() {
        List<String> coOrganizers = new ArrayList<>();
        coOrganizers.add("co-organizer-user");
        
        // Co-Organizer comments should be pinned automatically
        assertTrue(EventCommentPolicy.shouldPinComment("co-organizer-user", "main-organizer", coOrganizers));
        
        // Random user comments are not pinned
        assertFalse(EventCommentPolicy.shouldPinComment("random-user", "main-organizer", coOrganizers));
        
        // Main organizer comments are still pinned
        assertTrue(EventCommentPolicy.shouldPinComment("main-organizer", "main-organizer", coOrganizers));
    }

    @Test
    public void testCoOrganizerCanDeleteEntrantComments() {
        List<String> coOrganizers = new ArrayList<>();
        coOrganizers.add("co-organizer-user");

        EventComment entrantComment = new EventComment();
        entrantComment.setAuthorId("entrant-1");
        entrantComment.setStatus(EventComment.STATUS_ACTIVE);

        // A Co-Organizer should be allowed to delete a regular entrant's comment
        assertTrue(EventCommentPolicy.canDeleteComment(
                entrantComment, 
                "co-organizer-user", 
                "organizer", 
                "main-organizer", 
                coOrganizers
        ));

        // Ensure the Co-Organizer cannot delete their OWN comments using the delete button
        EventComment ownComment = new EventComment();
        ownComment.setAuthorId("co-organizer-user");
        ownComment.setStatus(EventComment.STATUS_ACTIVE);

        assertFalse(EventCommentPolicy.canDeleteComment(
                ownComment, 
                "co-organizer-user", 
                "organizer", 
                "main-organizer", 
                coOrganizers
        ));
    }
}
