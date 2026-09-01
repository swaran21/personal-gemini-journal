package com.pm.personalgeminijournalbackend.chat;

public record ChatTurn(Role role, String content) {
    public enum Role { USER, ASSISTANT }
}
