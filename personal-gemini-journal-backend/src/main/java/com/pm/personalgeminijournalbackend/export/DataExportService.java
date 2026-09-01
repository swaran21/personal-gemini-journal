package com.pm.personalgeminijournalbackend.export;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.personalgeminijournalbackend.journal.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class DataExportService {
    private final JournalRepository repository;
    private final ObjectMapper mapper;

    public DataExportService(JournalRepository repository, ObjectMapper mapper) { this.repository = repository; this.mapper = mapper; }

    public void write(String uid, Format format, OutputStream output) throws IOException {
        if (format == Format.JSON) writeJson(uid, output); else writeMarkdown(uid, output);
    }

    private void writeJson(String uid, OutputStream output) throws IOException {
        try (JsonGenerator json = mapper.getFactory().createGenerator(output)) {
            json.writeStartObject();
            json.writeStringField("exportedAt", Instant.now().toString());
            json.writeArrayFieldStart("journalEntries");
            visitEntries(uid, entry -> {
                json.writeStartObject(); json.writeStringField("id", entry.id()); json.writeStringField("content", entry.text());
                if (entry.response() != null) json.writeStringField("aiResponse", entry.response());
                json.writeStringField("createdAt", entry.createdAt().toString()); json.writeStringField("processingStatus", entry.processingStatus().name());
                if (entry.location() != null) { json.writeObjectFieldStart("location"); json.writeNumberField("latitude", entry.location().latitude()); json.writeNumberField("longitude", entry.location().longitude()); if (entry.location().label() != null) json.writeStringField("label", entry.location().label()); json.writeEndObject(); }
                json.writeEndObject();
            });
            json.writeEndArray(); json.writeArrayFieldStart("actionItems");
            visitActions(uid, item -> { json.writeStartObject(); json.writeStringField("id", item.id()); json.writeStringField("goal", item.text()); json.writeStringField("status", item.status().name()); json.writeStringField("createdAt", item.createdAt().toString()); json.writeEndObject(); });
            json.writeEndArray(); json.writeEndObject();
        }
    }

    private void writeMarkdown(String uid, OutputStream output) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        writer.write("# Personal Gemini Journal data export\n\nExported: " + Instant.now() + "\n\n## Journal entries\n\n");
        visitEntries(uid, entry -> {
            writer.write("### " + entry.createdAt() + "\n\n");
            if (entry.location() != null) writer.write("Location: " + safe(entry.location().label() == null ? entry.location().latitude() + ", " + entry.location().longitude() : entry.location().label()) + "\n\n");
            writer.write("**Journal**\n\n" + safe(entry.text()) + "\n\n");
            if (entry.response() != null) writer.write("**AI reflection**\n\n" + safe(entry.response()) + "\n\n");
        });
        writer.write("## Action items\n\n");
        visitActions(uid, item -> writer.write("- [" + (item.status() == ActionItem.Status.COMPLETED ? "x" : " ") + "] " + safe(item.text()) + " (`" + item.status() + "`, " + item.createdAt() + ")\n"));
        writer.flush();
    }

    private void visitEntries(String uid, IoConsumer<JournalEntry> visitor) throws IOException {
        String cursor = null;
        do { PageSlice<JournalEntry> page = repository.listEntries(uid, 100, cursor); for (JournalEntry entry : page.items()) visitor.accept(entry); cursor = page.nextCursor(); if (!page.hasMore()) return; } while (cursor != null);
    }
    private void visitActions(String uid, IoConsumer<ActionItem> visitor) throws IOException {
        String cursor = null;
        do { PageSlice<ActionItem> page = repository.listActionItems(uid, 100, cursor); for (ActionItem item : page.items()) visitor.accept(item); cursor = page.nextCursor(); if (!page.hasMore()) return; } while (cursor != null);
    }
    private String safe(String value) { return value.replace("\r", "").replace("<", "&lt;").replace(">", "&gt;"); }

    public enum Format { JSON, MARKDOWN }
    @FunctionalInterface private interface IoConsumer<T> { void accept(T value) throws IOException; }
}
