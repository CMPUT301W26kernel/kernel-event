package com.example.eventlottery;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for the WaitingListRepository class.
 * We mock the Firebase dependencies to verify method calls
 * and dependency injection.
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class WaitingListRepositoryTest {

    @Mock
    private FirebaseFirestore mockDb;
    @Mock
    private CollectionReference mockCollection;
    @Mock
    private DocumentReference mockDocRef;
    @Mock
    private Task mockTask;

    private WaitingListRepository repository;

    @Before
    public void setUp() {
        when(mockDb.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocRef);

        when(mockDb.runTransaction(any())).thenReturn(mockTask);

        when(mockDocRef.update(anyString(), any())).thenReturn(mockTask);
        when(mockDocRef.update(anyMap())).thenReturn(mockTask);
        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);
        when(mockTask.continueWith(any())).thenReturn(mockTask);

        repository = new WaitingListRepository(mockDb);
    }

    /**
     * Verifies that the joinWaitingList method exists and calls the database transaction.
     */
    @Test
    public void testJoinWaitingListCallsTransaction() {
        Task<Void> result = repository.joinWaitingList("testEvent", "testUser");
        assertNotNull(result);
        verify(mockDb).runTransaction(any());
    }

    /**
     * Verifies that the leaveWaitingList method exists and updates the document.
     */
    @Test
    public void testLeaveWaitingListCallsUpdate() {
        Task<Void> result = repository.leaveWaitingList("testEvent", "testUser");
        assertNotNull(result);
        verify(mockDocRef).update(anyMap());
    }

    /**
     * Verifies that the getWaitingList method exists and attempts to get the document.
     */
    @Test
    public void testGetWaitingListCallsGet() {
        // Mock the get method to return a Task
        when(mockDocRef.get()).thenReturn(mockTask);
        
        Task<?> result = repository.getWaitingList("testEvent");
        assertNotNull(result);
        verify(mockDocRef).get();
    }
}
