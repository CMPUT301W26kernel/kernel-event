package com.example.eventlottery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;


/**
 * NotificationRepositoryTest
 * Last Modified: 2026-04-03 by Radwa Sheikhdon
 *
 * Unit tests for NotificationRepository.
 * Verifies notification creation, transaction execution, and validation handling.
 */
public class NotificationRepositoryTest {

    private FirebaseFirestore mockDb;
    private CollectionReference mockCollection;
    private DocumentReference mockDocRef;
    private Task<Void> mockVoidTask;
    private Task<Void> mockTransactionTask;
    private NotificationRepository repository;

    /**
     * Sets up mocked Firestore dependencies and initializes repository.
     */
    @Before
    public void setup() {
        mockDb = mock(FirebaseFirestore.class);
        mockCollection = mock(CollectionReference.class);
        mockDocRef = mock(DocumentReference.class);
        mockVoidTask = mock(Task.class);
        mockTransactionTask = mock(Task.class);

        when(mockDb.collection(any())).thenReturn(mockCollection);

        // Mock notification creation chain
        when(mockCollection.document()).thenReturn(mockDocRef);
        when(mockDocRef.set(any(Notification.class))).thenReturn(mockVoidTask);

        when(mockVoidTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockVoidTask);

        // Mock transaction execution
        when(mockDb.runTransaction(any(Transaction.Function.class))).thenReturn(mockTransactionTask);
        when(mockTransactionTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTransactionTask);
        when(mockTransactionTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockTransactionTask);

        repository = spy(new NotificationRepository(mockDb));
    }

    /**
     * Tests that sendBulkNotification calls createNotification for each user.
     */
    @Test
    public void testSendBulkNotification_callsCreateNotification() {
        doNothing().when(repository).createNotification(any(Notification.class));

        repository.sendBulkNotification(
                Collections.singletonList("user1"),
                "event1",
                "Test message",
                Notification.TYPE_INFO
        );

        verify(repository, times(1)).createNotification(any(Notification.class));
    }

    /**
     * Tests that acceptInvitation triggers a Firestore transaction.
     */
    @Test
    public void testAcceptInvitation_callsRunTransaction() {
        Notification notification = new Notification();
        notification.setNotificationId("notif1");
        notification.setUserId("user1");
        notification.setEventId("event1");

        NotificationRepository.NotificationCallback callback =
                mock(NotificationRepository.NotificationCallback.class);

        repository.acceptInvitation(notification, callback);

        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }

    /**
     * Tests that declineInvitation triggers a Firestore transaction.
     */
    @Test
    public void testDeclineInvitation_callsRunTransaction() {
        Notification notification = new Notification();
        notification.setNotificationId("notif1");
        notification.setUserId("user1");
        notification.setEventId("event1");

        NotificationRepository.NotificationCallback callback =
                mock(NotificationRepository.NotificationCallback.class);

        repository.declineInvitation(notification, callback);

        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }

    /**
     * Tests that sendBulkNotification does nothing when the user list is empty.
     */
    @Test
    public void testSendBulkNotification_withEmptyList_doesNothing() {
        doNothing().when(repository).createNotification(any(Notification.class));

        repository.sendBulkNotification(
                Collections.emptyList(),
                "event1",
                "Test message",
                Notification.TYPE_INFO
        );

        verify(repository, never()).createNotification(any(Notification.class));
    }

    /**
     * Tests that acceptInvitation does not run a transaction for invalid notification data.
     */
    @Test
    public void testAcceptInvitation_invalidNotification_doesNotRunTransaction() {
        Notification notification = new Notification(); // missing required fields
        NotificationRepository.NotificationCallback callback =
                mock(NotificationRepository.NotificationCallback.class);

        repository.acceptInvitation(notification, callback);

        verify(mockDb, never()).runTransaction(any(Transaction.Function.class));
        verify(callback).onFailure(any(Exception.class));
    }

    /**
     * Tests that declineInvitation does not run a transaction for invalid notification data.
     */
    @Test
    public void testDeclineInvitation_invalidNotification_doesNotRunTransaction() {
        Notification notification = new Notification(); // missing required fields
        NotificationRepository.NotificationCallback callback =
                mock(NotificationRepository.NotificationCallback.class);

        repository.declineInvitation(notification, callback);

        verify(mockDb, never()).runTransaction(any(Transaction.Function.class));
        verify(callback).onFailure(any(Exception.class));
    }

    /**
     * Tests that markAsRead triggers failure when notificationId is missing.
     */
    @Test
    public void testMarkAsRead_invalidNotification_callsFailure() {
        Notification notification = new Notification(); // no notificationId
        NotificationRepository.NotificationCallback callback =
                mock(NotificationRepository.NotificationCallback.class);

        repository.markAsRead(notification, callback);

        verify(callback).onFailure(any(Exception.class));
    }

    /**
     * Tests that acceptCoOrganizerInvite triggers a Firestore transaction.
     */
    @Test
    public void testAcceptCoOrganizerInvite_callsRunTransaction() {
        Notification notification = new Notification();
        notification.setNotificationId("notif1");
        notification.setUserId("user1");
        notification.setEventId("event1");

        NotificationRepository.NotificationCallback callback =
                mock(NotificationRepository.NotificationCallback.class);

        repository.acceptCoOrganizerInvite(notification, callback);

        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }

    /**
     * Tests that declineCoOrganizerInvite triggers a Firestore transaction.
     */
    @Test
    public void testDeclineCoOrganizerInvite_callsRunTransaction() {
        Notification notification = new Notification();
        notification.setNotificationId("notif1");
        notification.setUserId("user1");
        notification.setEventId("event1");

        NotificationRepository.NotificationCallback callback =
                mock(NotificationRepository.NotificationCallback.class);

        repository.declineCoOrganizerInvite(notification, callback);

        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }
}

