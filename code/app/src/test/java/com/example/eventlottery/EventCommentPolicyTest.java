package com.example.eventlottery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

public class EventCommentPolicyTest {

    @Test
    public void entrantCanPostComments() {
        assertTrue(EventCommentPolicy.canPostComment("entrant-1", "entrant", "organizer-1", null));
    }

    @Test
    public void organizerCanOnlyPostOnOwnEvent() {
        assertTrue(EventCommentPolicy.canPostComment("organizer-1", "organizer", "organizer-1", null));
        assertFalse(EventCommentPolicy.canPostComment("organizer-2", "organizer", "organizer-1", null));
    }

    @Test
    public void adminCannotPostComments() {
        assertFalse(EventCommentPolicy.canPostComment("admin-1", "admin", "organizer-1", null));
    }

    @Test
    public void organizerCanDeleteAnyCommentOnTheirEvent() {
        EventComment entrantComment = new EventComment();
        entrantComment.setAuthorId("entrant-1");
        entrantComment.setStatus(EventComment.STATUS_ACTIVE);

        EventComment organizerComment = new EventComment();
        organizerComment.setAuthorId("organizer-1");
        organizerComment.setStatus(EventComment.STATUS_ACTIVE);

        // Can delete others
        assertTrue(EventCommentPolicy.canDeleteComment(
                entrantComment,
                "organizer-1",
                "organizer",
                "organizer-1",
                null
        ));
        // Can delete own
        assertTrue(EventCommentPolicy.canDeleteComment(
                organizerComment,
                "organizer-1",
                "organizer",
                "organizer-1",
                null
        ));
    }

    @Test
    public void adminCanDeleteAnyActiveComment() {
        EventComment comment = new EventComment();
        comment.setAuthorId("organizer-1");
        comment.setStatus(EventComment.STATUS_ACTIVE);

        assertTrue(EventCommentPolicy.canDeleteComment(
                comment,
                "admin-1",
                "admin",
                "organizer-1",
                null
        ));
    }

    @Test
    public void organizerCommentsArePinned() {
        assertTrue(EventCommentPolicy.shouldPinComment("organizer-1", "organizer-1", null));
        assertFalse(EventCommentPolicy.shouldPinComment("entrant-1", "organizer-1", null));
    }
}
