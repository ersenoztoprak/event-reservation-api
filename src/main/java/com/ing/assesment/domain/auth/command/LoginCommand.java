package com.ing.assesment.domain.auth.command;

public record LoginCommand(
        String email,
        String rawPassword) {
}
