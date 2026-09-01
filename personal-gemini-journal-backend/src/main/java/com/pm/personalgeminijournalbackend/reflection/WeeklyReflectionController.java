package com.pm.personalgeminijournalbackend.reflection;

import com.pm.personalgeminijournalbackend.security.FirebasePrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reflections")
public class WeeklyReflectionController {
    private final WeeklyReflectionService service;
    public WeeklyReflectionController(WeeklyReflectionService service) { this.service = service; }

    @PostMapping("/weekly")
    public WeeklyReflection generate(@AuthenticationPrincipal FirebasePrincipal principal, @Valid @RequestBody WeeklyReflectionRequest request) {
        return service.generate(principal.uid(), request);
    }
}
