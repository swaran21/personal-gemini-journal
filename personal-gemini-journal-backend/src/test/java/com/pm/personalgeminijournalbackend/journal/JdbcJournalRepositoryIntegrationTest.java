package com.pm.personalgeminijournalbackend.journal;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class JdbcJournalRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("test")
            .withInitScript("postgres-test-init.sql");

    private static JdbcJournalRepository repository;
    private static TransactionTemplate transactions;

    @BeforeAll
    static void initializeRepository() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser("journal_test");
        dataSource.setPassword("integration-test-only");
        Flyway.configure().dataSource(dataSource).load().migrate();

        JdbcClient client = JdbcClient.create(dataSource);
        repository = new JdbcJournalRepository(client);
        PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        transactions = new TransactionTemplate(transactionManager);
    }

    @Test
    void rowLevelSecurityAndUidPredicatesPreventCrossUserAccess() {
        String firstId = createComplete("user-a", "first", "reply-a", embedding(1d), Instant.parse("2026-01-01T00:00:00Z"));
        createComplete("user-b", "second", "reply-b", embedding(0.5d), Instant.parse("2026-01-02T00:00:00Z"));
        inTransaction(() -> {
            repository.saveActionItems("user-a", firstId, List.of("private goal"), Instant.now());
            return null;
        });
        String privateActionId = inTransaction(() -> repository.listActionItems("user-a").get(0).id());

        assertEquals(List.of("first"), inTransaction(() -> repository.listEntries("user-a")).stream().map(JournalEntry::text).toList());
        assertEquals(List.of("second"), inTransaction(() -> repository.listEntries("user-b")).stream().map(JournalEntry::text).toList());
        assertThrows(NoSuchElementException.class, () -> inTransaction(() -> {
            repository.setActionItemCompleted("user-b", privateActionId, true);
            return null;
        }));
    }

    @Test
    void vectorRetrievalReturnsOnlyTheAuthenticatedUsersMemory() {
        createComplete("owner", "closest", "reply", embedding(1d), Instant.now());
        createComplete("other", "must never leak", "reply", embedding(1d), Instant.now());

        List<JournalEntry> matches = inTransaction(() -> repository.findRelevant("owner", embedding(1d), 5));

        assertEquals(1, matches.size());
        assertEquals("closest", matches.get(0).text());
    }

    private static List<Double> embedding(double value) {
        return Collections.nCopies(768, value);
    }

    private static String createComplete(String uid, String text, String reply, List<Double> embedding, Instant createdAt) {
        String id = inTransaction(() -> repository.createPendingEntry(uid, text, createdAt));
        inTransaction(() -> { repository.completeEntryProcessing(uid, id, reply, embedding); return null; });
        return id;
    }

    private static <T> T inTransaction(java.util.concurrent.Callable<T> work) {
        return transactions.execute(status -> {
            try {
                return work.call();
            } catch (RuntimeException runtime) {
                throw runtime;
            } catch (Exception checked) {
                throw new IllegalStateException(checked);
            }
        });
    }
}
