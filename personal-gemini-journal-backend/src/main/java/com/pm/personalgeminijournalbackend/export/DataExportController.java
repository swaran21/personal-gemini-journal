package com.pm.personalgeminijournalbackend.export;

import com.pm.personalgeminijournalbackend.security.FirebasePrincipal;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.util.Locale;

@RestController
@RequestMapping("/api/user")
public class DataExportController {
    private final DataExportService service;
    public DataExportController(DataExportService service) { this.service = service; }

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> export(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @RequestParam(defaultValue = "json") String format) {
        DataExportService.Format selected;
        try { selected = DataExportService.Format.valueOf(format.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("format must be json or markdown"); }
        String extension = selected == DataExportService.Format.JSON ? "json" : "md";
        MediaType type = selected == DataExportService.Format.JSON ? MediaType.APPLICATION_JSON : new MediaType("text", "markdown");
        StreamingResponseBody body = output -> service.write(principal.uid(), selected, output);
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("personal-gemini-journal-" + LocalDate.now() + "." + extension).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }
}
