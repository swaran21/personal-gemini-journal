package com.pm.personalgeminijournalbackend.account;

import com.pm.personalgeminijournalbackend.security.FirebasePrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AccountDeletionService deletionService;
    public AccountController(AccountDeletionService deletionService) { this.deletionService = deletionService; }

    @DeleteMapping
    public AccountDeletionService.DeletionResult delete(@AuthenticationPrincipal FirebasePrincipal principal) {
        return deletionService.delete(principal.uid());
    }
}
