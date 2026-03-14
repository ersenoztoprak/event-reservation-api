package com.ing.assesment.infra.auth.config;

import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.auth.port.JwtTokenPort;
import com.ing.assesment.domain.auth.port.PasswordEncoderPort;
import com.ing.assesment.domain.auth.port.UserRepositoryPort;
import com.ing.assesment.infra.auth.persistence.adapter.UserRepositoryAdapter;
import com.ing.assesment.infra.auth.persistence.repository.UserJpaRepository;
import com.ing.assesment.infra.auth.security.adapter.BCryptPasswordEncoderAdapter;
import com.ing.assesment.infra.auth.security.adapter.CurrentUserAdapter;
import com.ing.assesment.infra.auth.security.jwt.JwtProperties;
import com.ing.assesment.infra.auth.security.jwt.JwtTokenAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AuthInfraConfig {

    @Bean
    public UserRepositoryPort userRepositoryPort(UserJpaRepository userJpaRepository) {
        return new UserRepositoryAdapter(userJpaRepository);
    }

    @Bean
    public PasswordEncoderPort passwordEncoderPort() {
        return new BCryptPasswordEncoderAdapter();
    }

    @Bean
    public JwtTokenPort jwtTokenPort(JwtProperties jwtProperties) {
        return new JwtTokenAdapter(jwtProperties);
    }

    @Bean
    public CurrentUserPort currentUserPort() {
        return new CurrentUserAdapter();
    }
}
