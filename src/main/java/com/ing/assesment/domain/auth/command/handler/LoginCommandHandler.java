package com.ing.assesment.domain.auth.command.handler;

import com.ing.assesment.domain.auth.command.LoginCommand;
import com.ing.assesment.domain.auth.model.TokenPair;
import com.ing.assesment.domain.auth.model.User;
import com.ing.assesment.domain.auth.port.JwtTokenPort;
import com.ing.assesment.domain.auth.port.PasswordEncoderPort;
import com.ing.assesment.domain.auth.port.UserRepositoryPort;
import com.ing.assesment.domain.common.CommandHandler;
import com.ing.assesment.domain.common.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class LoginCommandHandler implements CommandHandler<LoginCommand, TokenPair> {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final JwtTokenPort jwtTokenPort;

    @Override
    public TokenPair handle(LoginCommand command) {
        User user = userRepository.findByEmail(command.email()).orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches = passwordEncoder.matches(command.rawPassword(), user.getPasswordHash());
        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        user.markLoggedIn(Instant.now());
        userRepository.save(user);

        String accessToken = jwtTokenPort.generateAccessToken(user);
        String refreshToken = jwtTokenPort.generateRefreshToken(user);

        return TokenPair.bearer(accessToken, refreshToken);
    }
}
