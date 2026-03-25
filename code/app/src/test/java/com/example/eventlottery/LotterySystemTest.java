package com.example.eventlottery;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.example.eventlottery.lottery.LotterySystem;
import com.example.eventlottery.notifications.NotificationRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for the LotterySystem class.
 * Mocks Firebase dependencies to verify interaction with the database.
 */
@RunWith(MockitoJUnitRunner.class)
public class LotterySystemTest {

    @Mock
    private FirebaseFirestore mockDb;
    @Mock
    private NotificationRepository mockNotificationRepo;
    @Mock
    private CollectionReference mockCollection;
    @Mock
    private DocumentReference mockDocRef;
    @Mock
    private Task mockTask;

    private LotterySystem lotterySystem;

    @Before
    public void setUp() {
        when(mockDb.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocRef);

        when(mockDb.runTransaction(any())).thenReturn(mockTask);

        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);

        lotterySystem = new LotterySystem(mockDb, mockNotificationRepo);
    }

    /**
     * Verifies that the public API methods exist and invoke the database transaction.
     */
    @Test
    public void testDrawEntrantsCallsTransaction() {
        Task<?> result = lotterySystem.drawEntrants("testEvent", 5);
        assertNotNull("Result task should not be null", result);
        verify(mockDb).runTransaction(any());
    }

    @Test
    public void testAcceptInvitationCallsTransaction() {
        Task<Void> result = lotterySystem.acceptInvitation("testEvent", "testUser");
        assertNotNull("Result task should not be null", result);
        verify(mockDb).runTransaction(any());
    }

    @Test
    public void testDeclineOrCancelInvitationCallsTransaction() {
        Task<Void> result = lotterySystem.declineOrCancelInvitation("testEvent", "testUser");
        assertNotNull("Result task should not be null", result);
        verify(mockDb).runTransaction(any());
    }
}
