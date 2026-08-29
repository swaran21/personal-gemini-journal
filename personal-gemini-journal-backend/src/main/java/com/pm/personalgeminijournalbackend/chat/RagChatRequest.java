package com.pm.personalgeminijournalbackend.chat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record RagChatRequest(@NotBlank @Size(max = 4000) String question) { }
