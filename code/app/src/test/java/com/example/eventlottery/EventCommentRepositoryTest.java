package com.example.eventlottery;

import static org.junit.Assert.assertEquals;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class EventCommentRepositoryTest {

    @Test
    public void sortCommentsPinsOrganizerCommentsFirstThenNewest() {
        EventComment entrantOlder = buildComment("entrant-older", false, 100);
        EventComment entrantNewer = buildComment("entrant-newer", false, 200);
        EventComment organizerPinned = buildComment("organizer", true, 150);

        List<EventComment> sorted = EventCommentRepository.sortComments(
                Arrays.asList(entrantOlder, organizerPinned, entrantNewer)
        );

        assertEquals("organizer", sorted.get(0).getAuthorId());
        assertEquals("entrant-newer", sorted.get(1).getAuthorId());
        assertEquals("entrant-older", sorted.get(2).getAuthorId());
    }

    @Test
    public void sortCommentsPlacesNullTimestampsLastWithinSamePinBucket() {
        EventComment withTimestamp = buildComment("with-timestamp", false, 100);
        EventComment withoutTimestamp = new EventComment();
        withoutTimestamp.setAuthorId("without-timestamp");
        withoutTimestamp.setStatus(EventComment.STATUS_ACTIVE);
        withoutTimestamp.setPinned(false);

        List<EventComment> sorted = EventCommentRepository.sortComments(
                Arrays.asList(withoutTimestamp, withTimestamp)
        );

        assertEquals("with-timestamp", sorted.get(0).getAuthorId());
        assertEquals("without-timestamp", sorted.get(1).getAuthorId());
    }

    private EventComment buildComment(String authorId, boolean pinned, long seconds) {
        EventComment comment = new EventComment();
        comment.setAuthorId(authorId);
        comment.setPinned(pinned);
        comment.setStatus(EventComment.STATUS_ACTIVE);
        comment.setCreatedAt(new Timestamp(seconds, 0));
        return comment;
    }
}
