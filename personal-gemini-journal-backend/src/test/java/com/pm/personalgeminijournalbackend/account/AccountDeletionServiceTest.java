package com.pm.personalgeminijournalbackend.account;

import com.pm.personalgeminijournalbackend.journal.JournalRepository;
import com.pm.personalgeminijournalbackend.security.FirebasePrincipal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountDeletionServiceTest {
    @Test void controllerAndServiceDeriveOwnershipOnlyFromVerifiedPrincipal() {
        JournalRepository repository = mock(JournalRepository.class);
        AccountController controller = new AccountController(new LocalAccountDeletionService(repository));

        var result = controller.delete(new FirebasePrincipal("verified-uid"));

        assertTrue(result.dataDeleted());
        assertFalse(result.identityDeleted());
        assertEquals("KEYCLOAK", result.identityProvider());
        verify(repository).deleteAllUserData("verified-uid");
        verify(repository, never()).deleteAllUserData("other-user");
    }

    @Test void repositoryFailurePreventsSuccessfulDeletionResponse() {
        JournalRepository repository = mock(JournalRepository.class);
        doThrow(new IllegalStateException("database unavailable")).when(repository).deleteAllUserData("uid");
        var service = new LocalAccountDeletionService(repository);
        assertThrows(IllegalStateException.class, () -> service.delete("uid"));
    }
}
