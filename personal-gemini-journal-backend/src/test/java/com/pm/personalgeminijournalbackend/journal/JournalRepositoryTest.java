package com.pm.personalgeminijournalbackend.journal;

import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class JournalRepositoryTest {
    private final Firestore firestore = mock(Firestore.class);
    private final JournalRepository repository = new FirestoreJournalRepository(firestore);

    @Test void emptyActionItemBatchDoesNotTouchFirestore() {
        repository.saveActionItems("uid", List.of(), Instant.EPOCH);
        verifyNoInteractions(firestore);
    }

    @Test void rejectsInvalidActionItemIdsBeforeCreatingFirestorePaths() {
        for (String invalid : List.of("", "../other-user", "contains space", "a".repeat(129))) {
            assertThrows(IllegalArgumentException.class, () -> repository.setActionItemStatus("uid", invalid, ActionItem.Status.COMPLETED));
            assertThrows(IllegalArgumentException.class, () -> repository.deleteActionItem("uid", invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> repository.setActionItemStatus("uid", null, ActionItem.Status.COMPLETED));
        verifyNoInteractions(firestore);
    }
}
