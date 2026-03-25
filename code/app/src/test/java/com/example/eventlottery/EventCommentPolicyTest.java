package com.example.eventlottery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EventCommentPolicyTest {

    @Test
    public void entrantCanPostComments() {
        assertTrue(EventCommentPolicy.canPostComment("entrant-1", "entrant", "organizer-1"));
    }

    @Test
    public void organizerCanOnlyPostOnOwnEvent() {
        assertTrue(EventCommentPolicy.canPostComment("organizer-1", "organizer", "organizer-1"));
        assertFalse(EventCommentPolicy.canPostComment("organizer-2", "organizer", "organizer-1"));
    }

    @Test
    public void adminCannotPostComments() {
        assertFalse(EventCommentPolicy.canPostComment("admin-1", "admin", "organizer-1"));
    }

    @Test
    public void organizerCanDeleteEntrantCommentsButNotOwnComments() {
        EventComment entrantComment = new EventComment();
        entrantComment.setAuthorId("entrant-1");
        entrantComment.setStatus(EventComment.STATUS_ACTIVE);

        EventComment organizerComment = new EventComment();
        organizerComment.setAuthorId("organizer-1");
        organizerComment.setStatus(EventComment.STATUS_ACTIVE);

        assertTrue(EventCommentPolicy.canDeleteComment(
                entrantComment,
                "organizer-1",
                "organizer",
                "organizer-1"
        ));
        assertFalse(EventCommentPolicy.canDeleteComment(
                organizerComment,
                "organizer-1",
                "organizer",
                "organizer-1"
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
                "organizer-1"
        ));
    }

    @Test
    public void organizerCommentsArePinned() {
        assertTrue(EventCommentPolicy.shouldPinComment("organizer-1", "organizer-1"));
        assertFalse(EventCommentPolicy.shouldPinComment("entrant-1", "organizer-1"));
    }
}
