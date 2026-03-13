package com.ing.assesment.infra.common.configuration;

import com.ing.assesment.domain.auth.command.handler.LoginCommandHandler;
import com.ing.assesment.domain.auth.command.handler.RefreshTokenCommandHandler;
import com.ing.assesment.domain.auth.command.handler.RegisterUserCommandHandler;
import com.ing.assesment.domain.auth.port.JwtTokenPort;
import com.ing.assesment.domain.auth.port.PasswordEncoderPort;
import com.ing.assesment.domain.auth.port.UserRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//TODO move auth -> config
@Configuration
public class AuthBeanConfig {
    @Bean
    public RegisterUserCommandHandler registerUserService(
            UserRepositoryPort userRepository,
            PasswordEncoderPort passwordEncoder) {
        return new RegisterUserCommandHandler(userRepository, passwordEncoder);
    }

    @Bean
    public LoginCommandHandler loginCommandHandler(UserRepositoryPort userRepository,
                                                   PasswordEncoderPort passwordEncoder,
                                                   JwtTokenPort jwtTokenPort) {
        return new LoginCommandHandler(userRepository, passwordEncoder, jwtTokenPort);
    }

    @Bean
    public RefreshTokenCommandHandler refreshTokenCommandHandler(JwtTokenPort jwtTokenPort,
                                                                 UserRepositoryPort userRepository) {
        return new RefreshTokenCommandHandler(jwtTokenPort, userRepository);
    }
}
