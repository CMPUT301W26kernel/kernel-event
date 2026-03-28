package com.example.eventlottery;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for reading and moderating event comments.
 */
public class EventCommentRepository {

    /**
     * Callback used by listeners that observe changes to an event's comment thread.
     */
    interface CommentListener {
        void onCommentsChanged(List<EventComment> comments);
        void onError(Exception error);
    }

    private static final String TAG = "EventCommentRepo";
    private static final String EVENTS_COLLECTION = "events";
    private static final String COMMENTS_COLLECTION = "comments";

    private final FirebaseFirestore db;

    public EventCommentRepository() {
        this(FirebaseFirestore.getInstance());
    }

    EventCommentRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Starts a live listener for the event comment thread.
     *
     * @param eventId Event whose comments should be observed.
     * @param listener Callback receiving sorted comment updates or errors.
     * @return Registration handle that should be removed when the screen is destroyed.
     */
    public ListenerRegistration listenForComments(String eventId, CommentListener listener) {
        return commentsCollection(eventId).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Failed to listen for comments", error);
                listener.onError(error);
                return;
            }

            List<EventComment> comments = new ArrayList<>();
            if (snapshot != null) {
                comments.addAll(mapComments(snapshot));
            }

            listener.onCommentsChanged(sortComments(comments));
        });
    }

    /**
     * Creates a new comment document under the event's comments subcollection.
     *
     * @param eventId Event that owns the comment.
     * @param comment Comment payload to persist.
     * @return Task representing the asynchronous write.
     */
    public Task<Void> postComment(String eventId, EventComment comment) {
        DocumentReference commentRef = commentsCollection(eventId).document();
        comment.setCommentId(commentRef.getId());

        return commentRef.set(comment)
                .addOnFailureListener(error -> Log.e(TAG, "Failed to post comment", error));
    }

    /**
     * Soft-removes a comment by replacing its status and visible body metadata.
     *
     * @param eventId Event that owns the comment.
     * @param commentId Comment document to update.
     * @param removalReason Human-readable explanation shown in the UI.
     * @return Task representing the asynchronous update.
     */
    public Task<Void> removeComment(
            String eventId,
            String commentId,
            String removalReason
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", EventComment.STATUS_REMOVED);
        updates.put("removedReason", removalReason);

        return commentsCollection(eventId)
                .document(commentId)
                .update(updates)
                .addOnFailureListener(error -> Log.e(TAG, "Failed to remove comment", error));
    }

    /**
     * Sorts comments so pinned organizer comments appear first and newer comments appear before
     * older comments within the same pin bucket.
     *
     * @param comments Unsorted comments.
     * @return New list in display order.
     */
    static List<EventComment> sortComments(List<EventComment> comments) {
        List<EventComment> sorted = new ArrayList<>(comments);
        sorted.sort(
                Comparator.comparing(EventComment::isPinned).reversed()
                        .thenComparing(EventCommentRepository::compareCreatedAtDescending)
        );
        return sorted;
    }

    /**
     * Compares two comment timestamps in descending order while placing null timestamps last.
     *
     * @param left Left-side comment.
     * @param right Right-side comment.
     * @return Comparator result for descending timestamp order.
     */
    private static int compareCreatedAtDescending(EventComment left, EventComment right) {
        Timestamp leftTimestamp = left != null ? left.getCreatedAt() : null;
        Timestamp rightTimestamp = right != null ? right.getCreatedAt() : null;

        if (leftTimestamp == null && rightTimestamp == null) {
            return 0;
        }
        if (leftTimestamp == null) {
            return 1;
        }
        if (rightTimestamp == null) {
            return -1;
        }
        return rightTimestamp.compareTo(leftTimestamp);
    }

    /**
     * Maps Firestore query documents into typed comment models.
     *
     * @param snapshot Query snapshot returned from Firestore.
     * @return Materialized comment list.
     */
    private List<EventComment> mapComments(QuerySnapshot snapshot) {
        List<EventComment> comments = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            EventComment comment = document.toObject(EventComment.class);
            if (comment != null) {
                comment.setCommentId(document.getId());
                comments.add(comment);
            }
        }
        return comments;
    }

    /**
     * Returns the event-scoped comment subcollection reference.
     *
     * @param eventId Event whose comment collection should be addressed.
     * @return Firestore collection reference.
     */
    private CollectionReference commentsCollection(String eventId) {
        return db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .collection(COMMENTS_COLLECTION);
    }
}
