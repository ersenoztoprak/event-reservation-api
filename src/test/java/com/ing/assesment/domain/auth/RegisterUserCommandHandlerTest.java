package com.ing.assesment.domain.auth;

import com.ing.assesment.domain.auth.command.RegisterUserCommand;
import com.ing.assesment.domain.auth.command.handler.RegisterUserCommandHandler;
import com.ing.assesment.domain.auth.model.User;
import com.ing.assesment.domain.auth.port.PasswordEncoderPort;
import com.ing.assesment.domain.auth.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserCommandHandlerTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    private RegisterUserCommandHandler handler;

    @BeforeEach
    void setup() {
        handler = new RegisterUserCommandHandler(userRepository, passwordEncoder);
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        RegisterUserCommand command = new RegisterUserCommand("a@b.com", "pass");

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("hashed");

        handler.handle(command);

        verify(userRepository).save(argThat(u -> u.getEmail().equals("a@b.com") && u.getPasswordHash().equals("hashed")));
    }

    @Test
    void shouldThrowWhenDuplicateEmail() {
        RegisterUserCommand command = new RegisterUserCommand("dup@b.com", "pass");
        when(userRepository.findByEmail("dup@b.com")).thenReturn(Optional.of(mock(User.class)));

        assertThrows(IllegalArgumentException.class, () -> handler.handle(command));
    }
}
