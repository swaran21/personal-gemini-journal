package com.pm.personalgeminijournalbackend.chat;
import com.pm.personalgeminijournalbackend.gemini.*;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import com.pm.personalgeminijournalbackend.journal.JournalEntry;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {
 private static TemporalQueryResolver temporalQueries() { return new TemporalQueryResolver(Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC)); }
 private static ChatService service(GenerativeAiService gemini, EmbeddingService embeddings, JournalRepository repo, AccountabilityDispatcher accountability) { return new ChatService(gemini, embeddings, repo, accountability, temporalQueries()); }

 @Test void scopesHistoryAndSavesResultUnderAuthenticatedUid() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  when(repo.createPendingEntry(eq("uid-1"), eq("Plan my run"), isNull(), any())).thenReturn("entry-id");
  ChatResponse result = service(gemini, embeddings, repo, accountability).process("uid-1", " Plan my run ");
  assertNull(result.extractedGoal()); verify(repo).createPendingEntry(eq("uid-1"), eq("Plan my run"), isNull(), any()); verify(accountability).dispatch(eq("uid-1"), eq("entry-id"), eq("Plan my run"), any()); verifyNoInteractions(gemini, embeddings);
 }

 @Test void rejectsBlankOrControlOnlyEntriesBeforeCallingDependencies() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  ChatService chatService = service(gemini, embeddings, repo, accountability);
  assertThrows(IllegalArgumentException.class, () -> chatService.processJournalEntry("uid-1", "  \u0000  "));
  verifyNoInteractions(gemini, embeddings, repo, accountability);
 }

 @Test void removesNullCharactersAndReturnsSavedEntryContract() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  when(repo.createPendingEntry(eq("uid-1"), eq("Plan run"), isNull(), any())).thenReturn("entry-id");
  var response = service(gemini, embeddings, repo, accountability).processJournalEntry("uid-1", " Plan\u0000 run ");
  assertEquals("entry-id", response.id()); assertEquals("Plan run", response.content()); assertNull(response.aiResponse()); assertEquals(JournalEntry.ProcessingStatus.PENDING, response.processingStatus());
 }

 @Test void persistsOnlyExplicitlyProvidedValidatedLocation() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  var location = new com.pm.personalgeminijournalbackend.journal.GeoLocation(12.9716, 77.5946, "Campus");
  when(repo.createPendingEntry(eq("uid-1"), eq("Studied today"), eq(location), any())).thenReturn("entry-id");
  var response = service(gemini, embeddings, repo, accountability).processJournalEntry("uid-1", "Studied today", location);
  assertEquals(location, response.location()); verify(repo).createPendingEntry(eq("uid-1"), eq("Studied today"), eq(location), any());
 }

 @Test void ragRetrievalIsScopedToAuthenticatedUser() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  JournalEntry entry = new JournalEntry("entry-1", "I enjoyed a morning run", "Nice work", java.time.Instant.now(), List.of(0.1, 0.2), JournalEntry.ProcessingStatus.COMPLETED, null);
  when(embeddings.embed("What helped my running habit?")).thenReturn(List.of(0.1, 0.2)); when(repo.findRelevant("uid-1", List.of(0.1, 0.2), 5)).thenReturn(List.of(entry)); when(gemini.answerWithGrounding(any(RagContext.class))).thenReturn("Your past entry points to morning runs.");
  RagChatResponse response = service(gemini, embeddings, repo, accountability).chatWithPastSelf("uid-1", "What helped my running habit?");
  assertTrue(response.referencedEntries().get(0).endsWith("I enjoyed a morning run")); verify(repo, never()).findRelevant(eq("other-user"), anyList(), anyInt());
 }

 @Test void ragSanitizesQuestionAndBoundsReferencedExcerpt() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  String longText = "a".repeat(600);
  JournalEntry entry = new JournalEntry("entry-1", longText, "reply", java.time.Instant.now(), List.of(1d), JournalEntry.ProcessingStatus.COMPLETED, null);
  when(embeddings.embed("question")).thenReturn(List.of(1d)); when(repo.findRelevant("uid-1", List.of(1d), 5)).thenReturn(List.of(entry)); when(gemini.answerWithGrounding(any(RagContext.class))).thenReturn("Answer");
  RagChatResponse response = service(gemini, embeddings, repo, accountability).chatWithPastSelf("uid-1", " question\u0000 ");
  assertTrue(response.referencedEntries().get(0).length() > 500); assertTrue(response.referencedEntries().get(0).endsWith("..."));
 }

 @Test void retryUsesOnlyStoredContentOwnedByAuthenticatedUser() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  JournalEntry failed = new JournalEntry("entry-1", "stored private text", null, java.time.Instant.EPOCH, List.of(), JournalEntry.ProcessingStatus.FAILED, "unavailable");
  when(repo.retryEntryProcessing(eq("uid-1"), eq("entry-1"), any())).thenReturn(failed);

  var response = service(gemini, embeddings, repo, accountability).retryJournalEntry("uid-1", "entry-1");

  assertEquals(JournalEntry.ProcessingStatus.PENDING, response.processingStatus());
  verify(accountability).dispatch("uid-1", "entry-1", "stored private text", java.time.Instant.EPOCH);
  verify(repo, never()).retryEntryProcessing(eq("other-user"), anyString(), any());
 }

 @Test void temporalQuestionUsesAuthenticatedUsersExactTimeRangeWithoutEmbedding() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  JournalEntry entry = new JournalEntry("entry-1", "Built a RAG application", "Great progress", Instant.parse("2026-09-01T10:00:00Z"), List.of(), JournalEntry.ProcessingStatus.COMPLETED, null);
  when(repo.entriesBetween("uid-1", Instant.parse("2026-08-31T18:30:00Z"), Instant.parse("2026-09-01T12:00:00Z"), 100)).thenReturn(List.of(entry));
  when(gemini.answerWithGrounding(any(RagContext.class))).thenReturn("You built a RAG application.");

  RagChatResponse response = service(gemini, embeddings, repo, accountability).chatWithPastSelf("uid-1", new RagChatRequest("What did I do today?", "Asia/Kolkata", List.of()));

  assertEquals("You built a RAG application.", response.reply());
  verify(repo).entriesBetween("uid-1", Instant.parse("2026-08-31T18:30:00Z"), Instant.parse("2026-09-01T12:00:00Z"), 100);
  verifyNoInteractions(embeddings);
  verify(repo, never()).entriesBetween(eq("other-user"), any(), any(), anyInt());
 }

 @Test void passesBoundedSanitizedConversationToGroundedGenerator() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  when(embeddings.embed("What about that?")).thenReturn(List.of(1d)); when(repo.findRelevant("uid-1", List.of(1d), 5)).thenReturn(List.of()); when(gemini.answerWithGrounding(any(RagContext.class))).thenReturn("Answer");
  var request = new RagChatRequest("What about that?", "UTC", List.of(new RagChatRequest.HistoryMessage(RagChatRequest.Role.USER, " Earlier\u0000 question "), new RagChatRequest.HistoryMessage(RagChatRequest.Role.MODEL, "Earlier answer")));

  service(gemini, embeddings, repo, accountability).chatWithPastSelf("uid-1", request);

  var captor = org.mockito.ArgumentCaptor.forClass(RagContext.class); verify(gemini).answerWithGrounding(captor.capture());
  assertEquals("Earlier question", captor.getValue().conversation().get(0).content());
  assertEquals(ChatTurn.Role.ASSISTANT, captor.getValue().conversation().get(1).role());
  assertEquals(2, captor.getValue().conversation().size());
 }

 @Test void fallsBackToScopedTextSearchWhenEmbeddingQuotaIsUnavailable() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  JournalEntry entry = new JournalEntry("entry-1", "Dinner near Cubbon Park", "reply", Instant.EPOCH, List.of(), JournalEntry.ProcessingStatus.COMPLETED, null, new com.pm.personalgeminijournalbackend.journal.GeoLocation(12.9, 77.6, "Cubbon Park"));
  when(embeddings.embed("Where did I go near Cubbon Park?")).thenThrow(new IllegalStateException("quota"));
  when(repo.findTextRelevant("uid-1", "Where did I go near Cubbon Park?", 5)).thenReturn(List.of(entry));
  when(gemini.answerWithGrounding(any(RagContext.class))).thenReturn("You were near Cubbon Park.");
  assertEquals("You were near Cubbon Park.", service(gemini, embeddings, repo, accountability).chatWithPastSelf("uid-1", "Where did I go near Cubbon Park? ").reply());
  verify(repo).findTextRelevant("uid-1", "Where did I go near Cubbon Park?", 5);
 }
}
