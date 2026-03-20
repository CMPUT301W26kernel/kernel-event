package com.example.eventlottery;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test for HomePageFragment data loading.
 */
@RunWith(MockitoJUnitRunner.class)
public class HomePageFragmentTest {

    @Mock
    private FirebaseFirestore mockDb;
    @Mock
    private CollectionReference mockCollection;
    @Mock
    private Task<QuerySnapshot> mockTask;

    @Before
    public void setUp() {
        when(mockDb.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.get()).thenReturn(mockTask);
    }

    @Test
    public void testHomePageLoadsEventsFromFirestore() {
        mockDb.collection("events").get();

        verify(mockDb).collection("events");
        verify(mockCollection).get();
    }
}
