package com.pm.personalgeminijournalbackend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserIdentityController {
    @GetMapping("/me")
    public UserIdentity me(@AuthenticationPrincipal FirebasePrincipal principal, Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream().map(authority -> authority.getAuthority().replaceFirst("^ROLE_", "")).sorted().toList();
        return new UserIdentity(principal.uid(), roles);
    }

    public record UserIdentity(String uid, List<String> roles) { }
}
