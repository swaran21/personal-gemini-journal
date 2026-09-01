package com.pm.personalgeminijournalbackend.account;

public interface AccountDeletionService {
    DeletionResult delete(String uid);
    record DeletionResult(boolean dataDeleted, boolean identityDeleted, String identityProvider) { }
}
