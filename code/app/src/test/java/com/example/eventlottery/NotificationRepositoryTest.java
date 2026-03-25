package com.example.eventlottery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Notification Repository Test
 * Last Modified: 2026-03-12 by Radwa Sheikhdon
 * Handles creation and response logic for notifications.
 */
public class NotificationRepositoryTest {

    private FirebaseFirestore mockDb;
    private CollectionReference mockCollection;
    private DocumentReference mockDocRef;
    private Task<DocumentReference> mockAddTask;
    private Task<Void> mockTransactionTask;
    private NotificationRepository repository;

    /**
     * Sets up the test environment before each test case.
     */
    @Before
    public void setup() {
        mockDb = mock(FirebaseFirestore.class);
        mockCollection = mock(CollectionReference.class);
        mockDocRef = mock(DocumentReference.class);
        mockAddTask = mock(Task.class);
        mockTransactionTask = mock(Task.class);

        // Return the mock collection reference when collection() is called
        when(mockDb.collection(any())).thenReturn(mockCollection);

        // Return the mock document reference when document() is called
        when(mockCollection.add(any(Notification.class))).thenReturn(mockAddTask);
        when(mockAddTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockAddTask);
        when(mockAddTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockAddTask);

        // Return the mock task for .runTransaction()
        when(mockDb.runTransaction(any(Transaction.Function.class))).thenReturn(mockTransactionTask);
        when(mockTransactionTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTransactionTask);
        when(mockTransactionTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockTransactionTask);

        repository = new NotificationRepository(mockDb);
    }

    /**
     * Tests the createNotification method of the NotificationRepository.
     */
    @Test
    public void testSendBulkNotification_callsCreateNotification() {
        NotificationRepository spyRepo = spy(repository);
        spyRepo.sendBulkNotification(Collections.singletonList("user1"), "event1", "Test message");
        verify(spyRepo, times(1)).createNotification(any(Notification.class));
    }

    /**
     * Tests the acceptInvitation method of the NotificationRepository.
     */
    @Test
    public void testAcceptInvitation_callsRunTransaction() {
        Notification notification = new Notification();
        notification.setNotificationId("notif1");
        notification.setUserId("user1");
        notification.setEventId("event1");

        repository.acceptInvitation(notification);

        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }

    /**
     * Tests the declineInvitation method of the NotificationRepository.
     */
    @Test
    public void testDeclineInvitation_callsRunTransaction() {
        Notification notification = new Notification();
        notification.setNotificationId("notif1");
        notification.setUserId("user1");
        notification.setEventId("event1");

        repository.declineInvitation(notification);

        verify(mockDb).runTransaction(any(Transaction.Function.class));
    }
}

