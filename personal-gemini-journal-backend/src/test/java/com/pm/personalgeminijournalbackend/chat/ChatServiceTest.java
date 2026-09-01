package com.pm.personalgeminijournalbackend.chat;
import com.pm.personalgeminijournalbackend.gemini.*;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import com.pm.personalgeminijournalbackend.journal.JournalEntry;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {
 @Test void scopesHistoryAndSavesResultUnderAuthenticatedUid() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  when(repo.createPendingEntry(eq("uid-1"), eq("Plan my run"), isNull(), any())).thenReturn("entry-id");
  ChatResponse result = new ChatService(gemini, embeddings, repo, accountability).process("uid-1", " Plan my run ");
  assertNull(result.extractedGoal()); verify(repo).createPendingEntry(eq("uid-1"), eq("Plan my run"), isNull(), any()); verify(accountability).dispatch(eq("uid-1"), eq("entry-id"), eq("Plan my run"), any()); verifyNoInteractions(gemini, embeddings);
 }

 @Test void rejectsBlankOrControlOnlyEntriesBeforeCallingDependencies() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  ChatService service = new ChatService(gemini, embeddings, repo, accountability);
  assertThrows(IllegalArgumentException.class, () -> service.processJournalEntry("uid-1", "  \u0000  "));
  verifyNoInteractions(gemini, embeddings, repo, accountability);
 }

 @Test void removesNullCharactersAndReturnsSavedEntryContract() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  when(repo.createPendingEntry(eq("uid-1"), eq("Plan run"), isNull(), any())).thenReturn("entry-id");
  var response = new ChatService(gemini, embeddings, repo, accountability).processJournalEntry("uid-1", " Plan\u0000 run ");
  assertEquals("entry-id", response.id()); assertEquals("Plan run", response.content()); assertNull(response.aiResponse()); assertEquals(JournalEntry.ProcessingStatus.PENDING, response.processingStatus());
 }

 @Test void persistsOnlyExplicitlyProvidedValidatedLocation() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  var location = new com.pm.personalgeminijournalbackend.journal.GeoLocation(12.9716, 77.5946, "Campus");
  when(repo.createPendingEntry(eq("uid-1"), eq("Studied today"), eq(location), any())).thenReturn("entry-id");
  var response = new ChatService(gemini, embeddings, repo, accountability).processJournalEntry("uid-1", "Studied today", location);
  assertEquals(location, response.location()); verify(repo).createPendingEntry(eq("uid-1"), eq("Studied today"), eq(location), any());
 }

 @Test void ragRetrievalIsScopedToAuthenticatedUser() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  JournalEntry entry = new JournalEntry("entry-1", "I enjoyed a morning run", "Nice work", java.time.Instant.now(), List.of(0.1, 0.2), JournalEntry.ProcessingStatus.COMPLETED, null);
  when(embeddings.embed("What helped my running habit?")).thenReturn(List.of(0.1, 0.2)); when(repo.findRelevant("uid-1", List.of(0.1, 0.2), 5)).thenReturn(List.of(entry)); when(gemini.answerWithGrounding(eq("What helped my running habit?"), anyList())).thenReturn("Your past entry points to morning runs.");
  RagChatResponse response = new ChatService(gemini, embeddings, repo, accountability).chatWithPastSelf("uid-1", "What helped my running habit?");
  assertEquals("I enjoyed a morning run", response.referencedEntries().get(0)); verify(repo, never()).findRelevant(eq("other-user"), anyList(), anyInt());
 }

 @Test void ragSanitizesQuestionAndBoundsReferencedExcerpt() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  String longText = "a".repeat(600);
  JournalEntry entry = new JournalEntry("entry-1", longText, "reply", java.time.Instant.now(), List.of(1d), JournalEntry.ProcessingStatus.COMPLETED, null);
  when(embeddings.embed("question")).thenReturn(List.of(1d)); when(repo.findRelevant("uid-1", List.of(1d), 5)).thenReturn(List.of(entry)); when(gemini.answerWithGrounding(eq("question"), anyList())).thenReturn("Answer");
  RagChatResponse response = new ChatService(gemini, embeddings, repo, accountability).chatWithPastSelf("uid-1", " question\u0000 ");
  assertEquals(500, response.referencedEntries().get(0).length()); assertTrue(response.referencedEntries().get(0).endsWith("..."));
 }

 @Test void retryUsesOnlyStoredContentOwnedByAuthenticatedUser() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  JournalEntry failed = new JournalEntry("entry-1", "stored private text", null, java.time.Instant.EPOCH, List.of(), JournalEntry.ProcessingStatus.FAILED, "unavailable");
  when(repo.retryEntryProcessing(eq("uid-1"), eq("entry-1"), any())).thenReturn(failed);

  var response = new ChatService(gemini, embeddings, repo, accountability).retryJournalEntry("uid-1", "entry-1");

  assertEquals(JournalEntry.ProcessingStatus.PENDING, response.processingStatus());
  verify(accountability).dispatch("uid-1", "entry-1", "stored private text", java.time.Instant.EPOCH);
  verify(repo, never()).retryEntryProcessing(eq("other-user"), anyString(), any());
 }
}
