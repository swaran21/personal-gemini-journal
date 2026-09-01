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
        when(chatService.processJournalEntry("uid-1", "text", null)).thenReturn(created);
        when(repository.listEntries("uid-1", 20, null)).thenReturn(new PageSlice<>(List.of(new JournalEntry("id", "text", "reply", Instant.EPOCH, List.of(1d), JournalEntry.ProcessingStatus.COMPLETED, null)), null, false));

        var createResponse = controller.create(principal, new JournalEntryRequest("text", null));
        assertEquals(created, createResponse.getBody());
        assertEquals(202, createResponse.getStatusCode().value());
        PageSlice<JournalEntryResponse> entries = controller.entries(principal, 20, null);

        assertEquals("text", entries.items().get(0).content());
        assertEquals("reply", entries.items().get(0).aiResponse());
        assertNull(entries.items().get(0).extractedGoal());
        verify(chatService).processJournalEntry("uid-1", "text", null);
        verify(repository).listEntries("uid-1", 20, null);
    }

    @Test void actionControllerMapsStorageShapeAndStatus() {
        JournalRepository repository = mock(JournalRepository.class);
        JournalController controller = new JournalController(repository, mock(ActionItemService.class));
        when(repository.listActionItems("uid-1", 50, null)).thenReturn(new PageSlice<>(List.of(new ActionItem("id", "Write tests", ActionItem.Status.PROPOSED, Instant.EPOCH), new ActionItem("done", "Deploy", ActionItem.Status.COMPLETED, Instant.EPOCH)), null, false));

        PageSlice<ActionItemResponse> results = controller.actionItems(new FirebasePrincipal("uid-1"), 50, null);

        assertEquals(List.of("PROPOSED", "COMPLETED"), results.items().stream().map(ActionItemResponse::status).toList());
        assertEquals(List.of("Write tests", "Deploy"), results.items().stream().map(ActionItemResponse::goal).toList());
    }

    @Test void entryControllerAcceptsLocationButNeverAcceptsUid() {
        ChatService service = mock(ChatService.class); JournalRepository repository = mock(JournalRepository.class);
        JournalEntryController controller = new JournalEntryController(service, repository);
        GeoLocation location = new GeoLocation(12.9, 77.6, "Library");
        JournalEntryResponse response = new JournalEntryResponse("id", "text", null, null, Instant.EPOCH, JournalEntry.ProcessingStatus.PENDING, null, location);
        when(service.processJournalEntry("owner", "text", location)).thenReturn(response);
        assertEquals(location, controller.create(new FirebasePrincipal("owner"), new JournalEntryRequest("text", new JournalEntryRequest.LocationRequest(12.9, 77.6, "Library"))).getBody().location());
        verify(service, never()).processJournalEntry(eq("other-user"), anyString(), any());
    }

    @Test void paginationLimitsAreValidated() {
        JournalEntryController entries = new JournalEntryController(mock(ChatService.class), mock(JournalRepository.class));
        JournalController actions = new JournalController(mock(JournalRepository.class), mock(ActionItemService.class));
        FirebasePrincipal principal = new FirebasePrincipal("uid");
        assertThrows(IllegalArgumentException.class, () -> entries.entries(principal, 0, null));
        assertThrows(IllegalArgumentException.class, () -> actions.actionItems(principal, 101, null));
    }

    @Test void actionControllerChangesOnlyCurrentUsersItem() {
        JournalRepository repository = mock(JournalRepository.class);
        JournalController controller = new JournalController(repository, mock(ActionItemService.class));
        FirebasePrincipal principal = new FirebasePrincipal("uid-1");

        assertEquals(204, controller.update(principal, "item-1", new JournalController.CompletionRequest("COMPLETED")).getStatusCode().value());
        controller.update(principal, "item-1", new JournalController.CompletionRequest("PENDING"));
        controller.delete(principal, "item-1");

        verify(repository).setActionItemStatus("uid-1", "item-1", ActionItem.Status.COMPLETED);
        verify(repository).setActionItemStatus("uid-1", "item-1", ActionItem.Status.PENDING);
        verify(repository).deleteActionItem("uid-1", "item-1");
        verify(repository, never()).setActionItemStatus(eq("other-user"), anyString(), any());
    }

    @Test void completionRequestRejectsInvalidAndNullStatuses() {
        assertThrows(IllegalArgumentException.class, () -> new JournalController.CompletionRequest("complete"));
        assertThrows(IllegalArgumentException.class, () -> new JournalController.CompletionRequest(null));
    }

    @Test void manualGoalCreationUsesOnlyAuthenticatedPrincipalUid() {
        JournalRepository repository = mock(JournalRepository.class);
        ActionItemService service = mock(ActionItemService.class);
        JournalController controller = new JournalController(repository, service);
        ActionItem created = new ActionItem("id", "Practice system design", ActionItem.Status.PENDING, Instant.EPOCH);
        when(service.create("owner", "Practice system design")).thenReturn(created);

        var response = controller.createActionItem(new FirebasePrincipal("owner"), new CreateActionItemRequest("Practice system design"));

        assertEquals(201, response.getStatusCode().value());
        assertEquals("PENDING", response.getBody().status());
        verify(service).create("owner", "Practice system design");
        verify(service, never()).create(eq("other-user"), anyString());
    }

    @Test void retryEndpointForwardsOnlyAuthenticatedUid() {
        ChatService chatService = mock(ChatService.class);
        JournalRepository repository = mock(JournalRepository.class);
        JournalEntryController controller = new JournalEntryController(chatService, repository);
        var pending = new JournalEntryResponse("id", "stored", null, null, Instant.EPOCH, JournalEntry.ProcessingStatus.PENDING, null);
        when(chatService.retryJournalEntry("uid-1", "id")).thenReturn(pending);

        var response = controller.retry(new FirebasePrincipal("uid-1"), "id");

        assertEquals(202, response.getStatusCode().value());
        assertEquals(pending, response.getBody());
        verify(chatService, never()).retryJournalEntry(eq("other-user"), anyString());
    }
}
