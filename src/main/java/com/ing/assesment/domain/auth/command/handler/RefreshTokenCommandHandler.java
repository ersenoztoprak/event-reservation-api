package com.ing.assesment.domain.auth.command.handler;

import com.ing.assesment.domain.auth.command.RefreshTokenCommand;
import com.ing.assesment.domain.auth.model.TokenPair;
import com.ing.assesment.domain.auth.model.User;
import com.ing.assesment.domain.auth.port.JwtTokenPort;
import com.ing.assesment.domain.auth.port.UserRepositoryPort;
import com.ing.assesment.domain.common.CommandHandler;
import com.ing.assesment.domain.common.exception.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RefreshTokenCommandHandler implements CommandHandler<RefreshTokenCommand, TokenPair> {

    private final JwtTokenPort jwtTokenPort;
    private final UserRepositoryPort userRepository;

    @Override
    public TokenPair handle(RefreshTokenCommand command) {
        String refreshToken = command.refreshToken();

        if (!jwtTokenPort.isRefreshTokenValid(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        Long userId = jwtTokenPort.extractUserId(refreshToken);

        User user = userRepository.findById(userId).orElseThrow(InvalidRefreshTokenException::new);

        String newAccessToken = jwtTokenPort.generateAccessToken(user);

        return TokenPair.bearer(newAccessToken, refreshToken);
    }
}
