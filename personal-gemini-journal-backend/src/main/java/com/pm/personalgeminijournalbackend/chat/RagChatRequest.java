package com.pm.personalgeminijournalbackend.chat;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
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
    /** Accepts the public client shape { role: user|model, text: ... }; content/assistant remain backward compatible. */
    public record HistoryMessage(@NotNull Role role, @JsonAlias("text") @NotBlank @Size(max = 10000) String content) { }

    public enum Role {
        USER, MODEL;

        @JsonCreator
        public static Role fromJson(String value) {
            if (value == null) throw new IllegalArgumentException("history role is required");
            return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "user" -> USER;
                case "model", "assistant" -> MODEL;
                default -> throw new IllegalArgumentException("history role must be user or model");
            };
        }
    }
}
