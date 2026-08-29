package com.pm.personalgeminijournalbackend.security;

/** The only user identity accepted by application services. */
public record FirebasePrincipal(String uid) { }
