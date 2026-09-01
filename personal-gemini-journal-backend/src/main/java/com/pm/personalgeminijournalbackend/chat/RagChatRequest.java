package com.pm.personalgeminijournalbackend.chat;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RagChatRequest(
        @JsonAlias("query") @NotBlank @Size(max = 4000) String question,
        @Size(max = 64) String timeZone,
        @Valid @Size(max = 10) List<HistoryMessage> history) {
    public RagChatRequest(String question) { this(question, null, List.of()); }
    public RagChatRequest { history = history == null ? List.of() : List.copyOf(history); }
    public record HistoryMessage(@NotNull ChatTurn.Role role, @NotBlank @Size(max = 10000) String content) { }
}
