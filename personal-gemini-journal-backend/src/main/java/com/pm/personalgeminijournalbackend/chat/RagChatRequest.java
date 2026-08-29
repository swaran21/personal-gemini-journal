package com.pm.personalgeminijournalbackend.chat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonAlias;
public record RagChatRequest(@JsonAlias("query") @NotBlank @Size(max = 4000) String question) { }
