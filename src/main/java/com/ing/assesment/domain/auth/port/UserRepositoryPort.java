package com.ing.assesment.domain.auth.port;

import com.ing.assesment.domain.auth.model.User;

import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findByEmail(String email);

    User save(User user);

    Optional<User> findById(Long id);
}
