package com.example.eventlottery;



import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import java.util.HashMap;
import java.util.List;

public class NotificationRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Create Notification
    public void createNotification(Notification notification) {
        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> {
                    Log.d("NotificationRepo", "Notification added: " + docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("NotificationRepo", "Error adding notification", e);
                });
    }

    // Accept Invitation
    public void acceptInvitation(Notification notification) {
        db.runTransaction((Transaction.Function<Void>) transaction -> {

                    DocumentReference selectedRef = db.collection("events")
                            .document(notification.getEventId())
                            .collection("selected")
                            .document(notification.getUserId());

                    DocumentReference attendeeRef = db.collection("events")
                            .document(notification.getEventId())
                            .collection("attendees")
                            .document(notification.getUserId());

                    DocumentReference notificationRef = db.collection("notifications")
                            .document(notification.getNotificationId());

                    // Add to attendees
                    transaction.set(attendeeRef, new HashMap<>());

                    // Remove from selected
                    transaction.delete(selectedRef);

                    // Update notification status
                    transaction.update(notificationRef, "status", Notification.STATUS_ACCEPTED);

                    return null;
                }).addOnSuccessListener(aVoid ->
                        Log.d("NotificationRepo", "Invitation accepted successfully"))
                .addOnFailureListener(e ->
                        Log.e("NotificationRepo", "Failed to accept invitation", e));
    }

    // Decline Invitation
    public void declineInvitation(Notification notification) {
        db.runTransaction((Transaction.Function<Void>) transaction -> {

                    DocumentReference selectedRef = db.collection("events")
                            .document(notification.getEventId())
                            .collection("selected")
                            .document(notification.getUserId());

                    DocumentReference notificationRef = db.collection("notifications")
                            .document(notification.getNotificationId());

                    // Remove from selected
                    transaction.delete(selectedRef);

                    // Update notification status
                    transaction.update(notificationRef, "status", Notification.STATUS_DECLINED);

                    return null;
                }).addOnSuccessListener(aVoid ->
                        Log.d("NotificationRepo", "Invitation declined successfully"))
                .addOnFailureListener(e ->
                        Log.e("NotificationRepo", "Failed to decline invitation", e));
    }

    // Organizer bulk notifications to attendees

    public void sendBulkNotification(List<String> userIds, String eventId, String message) {
        for (String userId : userIds) {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setEventId(eventId);
            n.setType(Notification.TYPE_INFO);
            n.setMessage(message);
            n.setStatus(Notification.STATUS_UNREAD);
            n.setTimestamp(Timestamp.now());

            createNotification(n);
        }
    }
}