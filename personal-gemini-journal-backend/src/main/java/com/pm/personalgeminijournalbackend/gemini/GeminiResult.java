package com.pm.personalgeminijournalbackend.gemini;
import java.util.List;
public record GeminiResult(String reply, List<String> actionItems) { }
