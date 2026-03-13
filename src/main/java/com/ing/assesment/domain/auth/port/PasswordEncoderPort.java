package com.ing.assesment.domain.auth.port;

public interface PasswordEncoderPort {
    String encode(String s);

    boolean matches(String rawPassword, String encodedPassword);
}
