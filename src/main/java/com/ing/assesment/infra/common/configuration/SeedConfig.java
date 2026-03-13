package com.ing.assesment.infra.common.configuration;

import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.domain.auth.port.PasswordEncoderPort;
import com.ing.assesment.infra.auth.persistence.entity.UserEntity;
import com.ing.assesment.infra.auth.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SeedConfig {

    private final PasswordEncoderPort passwordEncoder;

    @Bean
    CommandLineRunner seedUsers(UserJpaRepository userRepository) {
        return args -> {

            createUserIfNotExists(
                    userRepository,
                    "admin@example.com",
                    "Admin123!",
                    Set.of(UserRole.ADMIN)
            );

            createUserIfNotExists(
                    userRepository,
                    "organizer@example.com",
                    "Organizer123!",
                    Set.of(UserRole.ORGANIZER)
            );

            createUserIfNotExists(
                    userRepository,
                    "customer@example.com",
                    "Customer123!",
                    Set.of(UserRole.CUSTOMER)
            );
        };
    }

    private void createUserIfNotExists(UserJpaRepository repo,
                                       String email,
                                       String password,
                                       Set<UserRole> roles) {

        repo.findByEmail(email).ifPresentOrElse(
                user -> {},
                () -> {
                    UserEntity user = new UserEntity();
                    user.setEmail(email);
                    user.setPasswordHash(passwordEncoder.encode(password));
                    user.setRoles(roles);
                    user.setCreatedAt(Instant.now());
                    repo.save(user);
                }
        );
    }
}
