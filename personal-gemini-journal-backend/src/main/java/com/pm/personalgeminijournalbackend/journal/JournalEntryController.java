package com.pm.personalgeminijournalbackend.journal;

import com.pm.personalgeminijournalbackend.chat.ChatService;
import com.pm.personalgeminijournalbackend.security.FirebasePrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/journal")
public class JournalEntryController {
    private final ChatService chatService;
    private final JournalRepository repository;
    public JournalEntryController(ChatService chatService, JournalRepository repository) { this.chatService = chatService; this.repository = repository; }
    @PostMapping("/entry") public ResponseEntity<JournalEntryResponse> create(@AuthenticationPrincipal FirebasePrincipal principal, @Valid @RequestBody JournalEntryRequest request) {
        GeoLocation location = request.location() == null ? null : request.location().toLocation();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(chatService.processJournalEntry(principal.uid(), request.content(), location));
    }
    @GetMapping("/entries") public PageSlice<JournalEntryResponse> entries(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor) {
        PageSlice<JournalEntry> page = repository.listEntries(principal.uid(), bounded(limit), cursor);
        return new PageSlice<>(page.items().stream().map(this::response).toList(), page.nextCursor(), page.hasMore());
    }
    @PostMapping("/entries/{id}/retry") public ResponseEntity<JournalEntryResponse> retry(@AuthenticationPrincipal FirebasePrincipal principal, @PathVariable String id) { return ResponseEntity.status(HttpStatus.ACCEPTED).body(chatService.retryJournalEntry(principal.uid(), id)); }
    @GetMapping("/calendar") public List<JournalEntryResponse> calendar(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "UTC") String timeZone) {
        if (year < 2000 || year > 2100 || month < 1 || month > 12) throw new IllegalArgumentException("year or month is outside the supported range");
        try {
            ZoneId zone = ZoneId.of(timeZone); YearMonth selected = YearMonth.of(year, month);
            return repository.entriesBetween(principal.uid(), selected.atDay(1).atStartOfDay(zone).toInstant(), selected.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant(), 100)
                    .stream().map(this::response).toList();
        } catch (DateTimeException exception) { throw new IllegalArgumentException("timeZone must be a valid IANA time zone"); }
    }
    @PatchMapping("/entries/{id}") public ResponseEntity<JournalEntryResponse> edit(@AuthenticationPrincipal FirebasePrincipal principal, @PathVariable String id, @Valid @RequestBody JournalEntryRequest request) {
        GeoLocation location = request.location() == null ? null : request.location().toLocation();
        JournalEntry updated = repository.updateEntryContent(principal.uid(), id, request.content().trim(), location, java.time.Instant.now());
        chatService.dispatchUpdatedEntry(principal.uid(), updated);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response(updated));
    }
    @DeleteMapping("/entries/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal FirebasePrincipal principal, @PathVariable String id) { repository.deleteJournalEntry(principal.uid(), id); return ResponseEntity.noContent().build(); }
    private int bounded(int limit) { if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100"); return limit; }
    private JournalEntryResponse response(JournalEntry entry) { return new JournalEntryResponse(entry.id(), entry.text(), entry.response(), null, entry.createdAt(), entry.processingStatus(), entry.processingError(), entry.location()); }
}
