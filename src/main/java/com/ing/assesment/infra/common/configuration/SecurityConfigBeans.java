package com.ing.assesment.infra.common.configuration;

import com.ing.assesment.domain.auth.port.PasswordEncoderPort;
import com.ing.assesment.infra.auth.security.adapter.BCryptPasswordEncoderAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfigBeans {
    @Bean
    public PasswordEncoderPort passwordEncoder() {
        return new BCryptPasswordEncoderAdapter();
    }
}
