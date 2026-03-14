package com.ing.assesment.infra.auth.config;

import com.ing.assesment.domain.auth.port.UserRepositoryPort;
import com.ing.assesment.infra.auth.persistence.adapter.UserRepositoryAdapter;
import com.ing.assesment.infra.auth.persistence.repository.UserJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfig {
    @Bean
    public UserRepositoryPort userRepository(UserJpaRepository userJpaRepository) {
        return new UserRepositoryAdapter(userJpaRepository);
    }
}
