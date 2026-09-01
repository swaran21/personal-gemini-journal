package com.pm.personalgeminijournalbackend.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.personalgeminijournalbackend.journal.*;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataExportServiceTest {
    @Test void jsonTakeoutExportsOnlyUserVisibleFieldsAndAuthenticatedOwnersPages() throws Exception {
        JournalRepository repository = mock(JournalRepository.class);
        JournalEntry entry = new JournalEntry("entry", "private text", "reflection", Instant.EPOCH, List.of(9d, 8d), JournalEntry.ProcessingStatus.COMPLETED, null, new GeoLocation(12.9, 77.6, "Library"));
        ActionItem action = new ActionItem("goal", "Write tests", ActionItem.Status.PENDING, Instant.EPOCH);
        when(repository.listEntries("owner", 100, null)).thenReturn(new PageSlice<>(List.of(entry), null, false));
        when(repository.listActionItems("owner", 100, null)).thenReturn(new PageSlice<>(List.of(action), null, false));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new DataExportService(repository, new ObjectMapper()).write("owner", DataExportService.Format.JSON, output);
        String json = output.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(json.contains("private text")); assertTrue(json.contains("Write tests")); assertTrue(json.contains("Library"));
        assertFalse(json.contains("embedding")); assertFalse(json.contains("owner"));
        verify(repository, never()).listEntries(eq("other-user"), anyInt(), any());
    }

    @Test void markdownEscapesHtmlAndTraversesCursorPages() throws Exception {
        JournalRepository repository = mock(JournalRepository.class);
        JournalEntry first = new JournalEntry("one", "<script>alert(1)</script>", null, Instant.EPOCH, List.of(), JournalEntry.ProcessingStatus.PENDING, null);
        when(repository.listEntries("owner", 100, null)).thenReturn(new PageSlice<>(List.of(first), "next", true));
        when(repository.listEntries("owner", 100, "next")).thenReturn(new PageSlice<>(List.of(), null, false));
        when(repository.listActionItems("owner", 100, null)).thenReturn(new PageSlice<>(List.of(), null, false));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new DataExportService(repository, new ObjectMapper()).write("owner", DataExportService.Format.MARKDOWN, output);
        String markdown = output.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(markdown.contains("&lt;script&gt;")); assertFalse(markdown.contains("<script>")); verify(repository).listEntries("owner", 100, "next");
    }
}
