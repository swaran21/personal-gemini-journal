package com.pm.personalgeminijournalbackend.journal;

import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class ActionItemService {
    private final JournalRepository repository;
    private final Clock clock;

    public ActionItemService(JournalRepository repository) {
        this(repository, Clock.systemUTC());
    }

    ActionItemService(JournalRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public ActionItem create(String uid, String rawGoal) {
        String goal = rawGoal == null ? "" : rawGoal.replace("\u0000", "").replaceAll("\\s+", " ").trim();
        if (goal.isBlank()) throw new IllegalArgumentException("goal must contain non-whitespace text");
        if (goal.length() > 1000) throw new IllegalArgumentException("goal must not exceed 1000 characters");
        return repository.createActionItem(uid, goal, clock.instant());
    }
}
