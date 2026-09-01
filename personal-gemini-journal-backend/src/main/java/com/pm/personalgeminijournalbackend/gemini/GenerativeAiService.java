package com.pm.personalgeminijournalbackend.gemini;

import com.pm.personalgeminijournalbackend.journal.JournalEntry;
import java.util.List;
import com.pm.personalgeminijournalbackend.reflection.WeeklyReflection;

public interface GenerativeAiService {
    GeminiResult reflect(String entry, List<JournalEntry> history);
    List<String> extractActionItems(String entry);
    String answerWithGrounding(String question, List<JournalEntry> entries);
    WeeklyReflection generateWeeklyReflection(List<JournalEntry> entries);
}
