package com.ing.assesment.domain.auth.command.handler;

import com.ing.assesment.domain.auth.command.RegisterUserCommand;
import com.ing.assesment.domain.auth.model.User;
import com.ing.assesment.domain.auth.port.PasswordEncoderPort;
import com.ing.assesment.domain.auth.port.UserRepositoryPort;
import com.ing.assesment.domain.common.VoidCommandHandler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RegisterUserCommandHandler implements VoidCommandHandler<RegisterUserCommand> {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;

    @Override
    public void handle(RegisterUserCommand command) {

        checkDuplicateEmail(command);

        String hash = passwordEncoder.encode(command.rawPassword());

        User user = User.create(command.email(), hash);

        userRepository.save(user);
    }

    private void checkDuplicateEmail(RegisterUserCommand command) {
        userRepository.findByEmail(command.email())
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Email already registered");
                });
    }
}
