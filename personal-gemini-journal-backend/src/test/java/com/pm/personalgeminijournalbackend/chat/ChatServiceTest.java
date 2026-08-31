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
  when(repo.recentEntries("uid-1", 10)).thenReturn(List.of()); when(gemini.reflect(eq("Plan my run"), anyList())).thenReturn(new GeminiResult("That sounds achievable.", List.of())); when(embeddings.embed("Plan my run")).thenReturn(List.of(0.1, 0.2));
  ChatResponse result = new ChatService(gemini, embeddings, repo, accountability).process("uid-1", " Plan my run ");
  assertNull(result.extractedGoal()); verify(repo).saveEntry(eq("uid-1"), eq("Plan my run"), anyString(), anyList(), any()); verify(accountability).dispatch(eq("uid-1"), any(), eq("Plan my run"), any()); verify(repo, never()).recentEntries(eq("other-user"), anyInt());
 }

 @Test void rejectsBlankOrControlOnlyEntriesBeforeCallingDependencies() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  ChatService service = new ChatService(gemini, embeddings, repo, accountability);
  assertThrows(IllegalArgumentException.class, () -> service.processJournalEntry("uid-1", "  \u0000  "));
  verifyNoInteractions(gemini, embeddings, repo, accountability);
 }

 @Test void removesNullCharactersAndReturnsSavedEntryContract() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  when(repo.recentEntries("uid-1", 10)).thenReturn(List.of()); when(gemini.reflect(eq("Plan run"), anyList())).thenReturn(new GeminiResult("Reply", List.of())); when(embeddings.embed("Plan run")).thenReturn(List.of(1d)); when(repo.saveEntry(eq("uid-1"), eq("Plan run"), eq("Reply"), anyList(), any())).thenReturn("entry-id");
  var response = new ChatService(gemini, embeddings, repo, accountability).processJournalEntry("uid-1", " Plan\u0000 run ");
  assertEquals("entry-id", response.id()); assertEquals("Plan run", response.content()); assertEquals("Reply", response.aiResponse()); assertNull(response.extractedGoal());
 }

 @Test void ragRetrievalIsScopedToAuthenticatedUser() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  JournalEntry entry = new JournalEntry("entry-1", "I enjoyed a morning run", "Nice work", java.time.Instant.now(), List.of(0.1, 0.2));
  when(embeddings.embed("What helped my running habit?")).thenReturn(List.of(0.1, 0.2)); when(repo.findRelevant("uid-1", List.of(0.1, 0.2), 5)).thenReturn(List.of(entry)); when(gemini.answerWithGrounding(eq("What helped my running habit?"), anyList())).thenReturn("Your past entry points to morning runs.");
  RagChatResponse response = new ChatService(gemini, embeddings, repo, accountability).chatWithPastSelf("uid-1", "What helped my running habit?");
  assertEquals("I enjoyed a morning run", response.referencedEntries().get(0)); verify(repo, never()).findRelevant(eq("other-user"), anyList(), anyInt());
 }

 @Test void ragSanitizesQuestionAndBoundsReferencedExcerpt() {
  GenerativeAiService gemini = mock(GenerativeAiService.class); EmbeddingService embeddings = mock(EmbeddingService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityDispatcher accountability = mock(AccountabilityDispatcher.class);
  String longText = "a".repeat(600);
  JournalEntry entry = new JournalEntry("entry-1", longText, "reply", java.time.Instant.now(), List.of(1d));
  when(embeddings.embed("question")).thenReturn(List.of(1d)); when(repo.findRelevant("uid-1", List.of(1d), 5)).thenReturn(List.of(entry)); when(gemini.answerWithGrounding(eq("question"), anyList())).thenReturn("Answer");
  RagChatResponse response = new ChatService(gemini, embeddings, repo, accountability).chatWithPastSelf("uid-1", " question\u0000 ");
  assertEquals(500, response.referencedEntries().get(0).length()); assertTrue(response.referencedEntries().get(0).endsWith("..."));
 }
}
