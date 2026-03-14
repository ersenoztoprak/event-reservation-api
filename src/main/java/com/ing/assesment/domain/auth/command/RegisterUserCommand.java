package com.ing.assesment.domain.auth.command;

public record RegisterUserCommand(
        String email,
        String rawPassword) {

}
