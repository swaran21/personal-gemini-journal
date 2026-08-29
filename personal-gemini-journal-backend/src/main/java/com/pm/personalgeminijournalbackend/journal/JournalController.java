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
    @GetMapping("/journal-entries") public List<JournalEntry> entries(@AuthenticationPrincipal FirebasePrincipal p) { return repository.listEntries(p.uid()); }
    @GetMapping("/action-items") public List<ActionItemResponse> actionItems(@AuthenticationPrincipal FirebasePrincipal p) { return repository.listActionItems(p.uid()).stream().map(item -> new ActionItemResponse(item.id(), item.text(), item.completed() ? "COMPLETED" : "PENDING", item.createdAt())).toList(); }
    @PatchMapping("/action-items/{id}") public ResponseEntity<Void> update(@AuthenticationPrincipal FirebasePrincipal p, @PathVariable String id, @RequestBody @NotNull CompletionRequest body) { repository.setActionItemCompleted(p.uid(), id, "COMPLETED".equals(body.status())); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/action-items/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal FirebasePrincipal p, @PathVariable String id) { repository.deleteActionItem(p.uid(), id); return ResponseEntity.noContent().build(); }
    public record CompletionRequest(String status) {
        public CompletionRequest {
            if (!"PENDING".equals(status) && !"COMPLETED".equals(status)) throw new IllegalArgumentException("status must be PENDING or COMPLETED");
        }
    }
}
