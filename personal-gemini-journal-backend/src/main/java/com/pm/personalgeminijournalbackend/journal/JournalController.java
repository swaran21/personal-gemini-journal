package com.pm.personalgeminijournalbackend.journal;

import com.pm.personalgeminijournalbackend.security.FirebasePrincipal;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Backend alternatives to direct browser Firestore access. */
@RestController @RequestMapping("/api")
public class JournalController {
    private final JournalRepository repository;
    public JournalController(JournalRepository repository) { this.repository = repository; }
    @GetMapping("/journal-entries") public PageSlice<JournalEntryResponse> entries(@AuthenticationPrincipal FirebasePrincipal p, @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor) {
        PageSlice<JournalEntry> page = repository.listEntries(p.uid(), bounded(limit), cursor);
        return new PageSlice<>(page.items().stream().map(entry -> new JournalEntryResponse(entry.id(), entry.text(), entry.response(), null, entry.createdAt(), entry.processingStatus(), entry.processingError(), entry.location())).toList(), page.nextCursor(), page.hasMore());
    }
    @GetMapping("/action-items") public PageSlice<ActionItemResponse> actionItems(@AuthenticationPrincipal FirebasePrincipal p, @RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) String cursor) {
        PageSlice<ActionItem> page = repository.listActionItems(p.uid(), bounded(limit), cursor);
        return new PageSlice<>(page.items().stream().map(item -> new ActionItemResponse(item.id(), item.text(), item.status().name(), item.createdAt())).toList(), page.nextCursor(), page.hasMore());
    }
    @PatchMapping("/action-items/{id}") public ResponseEntity<Void> update(@AuthenticationPrincipal FirebasePrincipal p, @PathVariable String id, @RequestBody @NotNull CompletionRequest body) { repository.setActionItemStatus(p.uid(), id, ActionItem.Status.valueOf(body.status())); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/action-items/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal FirebasePrincipal p, @PathVariable String id) { repository.deleteActionItem(p.uid(), id); return ResponseEntity.noContent().build(); }
    public record CompletionRequest(String status) {
        public CompletionRequest {
            if (!"PENDING".equals(status) && !"COMPLETED".equals(status)) throw new IllegalArgumentException("status must be PENDING or COMPLETED");
        }
    }
    private int bounded(int limit) { if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100"); return limit; }
}
