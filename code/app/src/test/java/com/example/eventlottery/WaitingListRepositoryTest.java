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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;

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

        repository = new WaitingListRepository(mockDb);
    }

    /**
     * Verifies that the joinWaitingList method exists and calls the database transaction.
     */
    @Test
    public void testJoinWaitingListCallsTransaction() {
        when(mockDb.runTransaction(any())).thenReturn(mockTask);
        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);

        Task<Void> result = repository.joinWaitingList("testEvent", "testUser");
        assertNotNull(result);
        verify(mockDb).runTransaction(any());
    }

    /**
     * Verifies that the leaveWaitingList method exists and updates the document.
     */
    @Test
    public void testLeaveWaitingListCallsUpdate() {
        when(mockDocRef.update(anyMap())).thenReturn(mockTask);
        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);

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
        when(mockTask.continueWith(any())).thenReturn(mockTask);
        
        Task<?> result = repository.getWaitingList("testEvent");
        assertNotNull(result);
        verify(mockDocRef).get();
    }

    @Test
    public void testValidateJoinEligibility_allowsFreshEntrant() {
        String error = WaitingListRepository.validateJoinEligibility(
                "entrant-1",
                "organizer-1",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertNull(error);
    }

    @Test
    public void testValidateJoinEligibility_rejectsAlreadyInvitedEntrant() {
        String error = WaitingListRepository.validateJoinEligibility(
                "entrant-1",
                "organizer-1",
                Collections.emptyList(),
                Collections.singletonList("entrant-1"),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertEquals("User has already been invited to this event.", error);
    }

    @Test
    public void testValidateJoinEligibility_rejectsCancelledEntrant() {
        String error = WaitingListRepository.validateJoinEligibility(
                "entrant-1",
                "organizer-1",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("entrant-1"),
                Collections.emptyList()
        );

        assertEquals("User has already responded to this event and cannot rejoin.", error);
    }

    @Test
    public void testValidateJoinEligibility_rejectsOrganizerAndCoOrganizer() {
        String organizerError = WaitingListRepository.validateJoinEligibility(
                "organizer-1",
                "organizer-1",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        String coOrganizerError = WaitingListRepository.validateJoinEligibility(
                "co-organizer-1",
                "organizer-1",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList("co-organizer-1")
        );

        assertEquals("Organizers cannot join their own event's waiting list.", organizerError);
        assertEquals("Organizers cannot join their own event's waiting list.", coOrganizerError);
    }
}
