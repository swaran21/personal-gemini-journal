package com.pm.personalgeminijournalbackend.journal;

import com.pm.personalgeminijournalbackend.chat.ChatService;
import com.pm.personalgeminijournalbackend.security.FirebasePrincipal;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JournalControllerTest {
    @Test void entryControllerForwardsOnlyPrincipalUidAndMapsStoredEntries() {
        ChatService chatService = mock(ChatService.class);
        JournalRepository repository = mock(JournalRepository.class);
        JournalEntryController controller = new JournalEntryController(chatService, repository);
        FirebasePrincipal principal = new FirebasePrincipal("uid-1");
        JournalEntryResponse created = new JournalEntryResponse("id", "text", null, null, Instant.EPOCH, JournalEntry.ProcessingStatus.PENDING, null);
        when(chatService.processJournalEntry("uid-1", "text")).thenReturn(created);
        when(repository.listEntries("uid-1")).thenReturn(List.of(new JournalEntry("id", "text", "reply", Instant.EPOCH, List.of(1d), JournalEntry.ProcessingStatus.COMPLETED, null)));

        var createResponse = controller.create(principal, new JournalEntryRequest("text"));
        assertEquals(created, createResponse.getBody());
        assertEquals(202, createResponse.getStatusCode().value());
        List<JournalEntryResponse> entries = controller.entries(principal);

        assertEquals("text", entries.get(0).content());
        assertEquals("reply", entries.get(0).aiResponse());
        assertNull(entries.get(0).extractedGoal());
        verify(chatService).processJournalEntry("uid-1", "text");
        verify(repository).listEntries("uid-1");
    }

    @Test void actionControllerMapsStorageShapeAndStatus() {
        JournalRepository repository = mock(JournalRepository.class);
        JournalController controller = new JournalController(repository);
        when(repository.listActionItems("uid-1")).thenReturn(List.of(new ActionItem("id", "Write tests", false, Instant.EPOCH), new ActionItem("done", "Deploy", true, Instant.EPOCH)));

        List<ActionItemResponse> results = controller.actionItems(new FirebasePrincipal("uid-1"));

        assertEquals(List.of("PENDING", "COMPLETED"), results.stream().map(ActionItemResponse::status).toList());
        assertEquals(List.of("Write tests", "Deploy"), results.stream().map(ActionItemResponse::goal).toList());
    }

    @Test void actionControllerChangesOnlyCurrentUsersItem() {
        JournalRepository repository = mock(JournalRepository.class);
        JournalController controller = new JournalController(repository);
        FirebasePrincipal principal = new FirebasePrincipal("uid-1");

        assertEquals(204, controller.update(principal, "item-1", new JournalController.CompletionRequest("COMPLETED")).getStatusCode().value());
        controller.update(principal, "item-1", new JournalController.CompletionRequest("PENDING"));
        controller.delete(principal, "item-1");

        verify(repository).setActionItemCompleted("uid-1", "item-1", true);
        verify(repository).setActionItemCompleted("uid-1", "item-1", false);
        verify(repository).deleteActionItem("uid-1", "item-1");
        verify(repository, never()).setActionItemCompleted(eq("other-user"), anyString(), anyBoolean());
    }

    @Test void completionRequestRejectsInvalidAndNullStatuses() {
        assertThrows(IllegalArgumentException.class, () -> new JournalController.CompletionRequest("complete"));
        assertThrows(IllegalArgumentException.class, () -> new JournalController.CompletionRequest(null));
    }
}
