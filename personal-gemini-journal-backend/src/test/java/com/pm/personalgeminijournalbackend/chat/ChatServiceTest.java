package com.pm.personalgeminijournalbackend.chat;
import com.pm.personalgeminijournalbackend.gemini.*;
import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {
 @Test void scopesHistoryAndSavesResultUnderAuthenticatedUid() {
  GeminiService gemini = mock(GeminiService.class); JournalRepository repo = mock(JournalRepository.class); AccountabilityService accountability = mock(AccountabilityService.class);
  when(repo.recentEntries("uid-1", 10)).thenReturn(List.of()); when(gemini.reflect(eq("Plan my run"), anyList())).thenReturn(new GeminiResult("That sounds achievable.", List.of("Run 5 km"))); when(gemini.embed("Plan my run")).thenReturn(List.of(0.1, 0.2));
  ChatResponse result = new ChatService(gemini, repo, accountability).process("uid-1", " Plan my run ");
  assertEquals("Run 5 km", result.extractedGoal()); verify(repo).saveEntry(eq("uid-1"), eq("Plan my run"), anyString(), anyList(), any()); verify(accountability).persist(eq("uid-1"), eq(List.of("Run 5 km")), any()); verify(repo, never()).recentEntries(eq("other-user"), anyInt());
 }
}
