package com.pm.personalgeminijournalbackend.journal;

import com.pm.personalgeminijournalbackend.chat.ChatService;
import com.pm.personalgeminijournalbackend.security.FirebasePrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/journal")
public class JournalEntryController {
    private final ChatService chatService;
    private final JournalRepository repository;
    public JournalEntryController(ChatService chatService, JournalRepository repository) { this.chatService = chatService; this.repository = repository; }
    @PostMapping("/entry") public ResponseEntity<JournalEntryResponse> create(@AuthenticationPrincipal FirebasePrincipal principal, @Valid @RequestBody JournalEntryRequest request) { return ResponseEntity.status(HttpStatus.ACCEPTED).body(chatService.processJournalEntry(principal.uid(), request.content())); }
    @GetMapping("/entries") public List<JournalEntryResponse> entries(@AuthenticationPrincipal FirebasePrincipal principal) { return repository.listEntries(principal.uid()).stream().map(entry -> new JournalEntryResponse(entry.id(), entry.text(), entry.response(), null, entry.createdAt(), entry.processingStatus(), entry.processingError())).toList(); }
}
