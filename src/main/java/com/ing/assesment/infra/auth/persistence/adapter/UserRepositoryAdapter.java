package com.ing.assesment.infra.auth.persistence.adapter;

import com.ing.assesment.domain.auth.model.User;
import com.ing.assesment.domain.auth.port.UserRepositoryPort;
import com.ing.assesment.infra.auth.persistence.entity.UserEntity;
import com.ing.assesment.infra.auth.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        UserEntity entity;

        if (user.getId() != null) {
            entity = userJpaRepository.findById(user.getId()).orElse(new UserEntity());
        } else {
            entity = new UserEntity();
        }

        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRoles(user.getRoles());
        entity.setLastLoginAt(user.getLastLoginAt());
        UserEntity saved = userJpaRepository.save(entity);

        User mapped = User.create(saved.getEmail(), saved.getPasswordHash());
        mapped.setId(saved.getId());
        mapped.getRoles().clear();
        mapped.getRoles().addAll(saved.getRoles());
        mapped.setCreatedAt(saved.getCreatedAt());
        mapped.setLastLoginAt(saved.getLastLoginAt());

        return mapped;
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(this::toDomain);
    }

    private User toDomain(UserEntity entity) {
        User user = User.create(entity.getEmail(), entity.getPasswordHash());
        user.setId(entity.getId());
        user.getRoles().clear();
        user.getRoles().addAll(entity.getRoles());
        user.setCreatedAt(entity.getCreatedAt());
        user.setLastLoginAt(entity.getLastLoginAt());
        return user;
    }
}
