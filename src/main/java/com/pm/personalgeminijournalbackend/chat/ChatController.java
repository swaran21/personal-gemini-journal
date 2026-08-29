package com.pm.personalgeminijournalbackend.chat;
import com.pm.personalgeminijournalbackend.security.FirebasePrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    public ChatController(ChatService chatService) { this.chatService = chatService; }
    @PostMapping public ResponseEntity<ChatResponse> chat(@AuthenticationPrincipal FirebasePrincipal principal, @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.process(principal.uid(), request.entry()));
    }
}
