package com.pm.personalgeminijournalbackend.chat;
import java.util.List;
public record RagChatResponse(String reply, List<String> referencedEntries) { }
