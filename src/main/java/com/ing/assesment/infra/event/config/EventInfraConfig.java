package com.ing.assesment.infra.event.config;

import com.ing.assesment.domain.event.port.EventRepositoryPort;
import com.ing.assesment.infra.event.persistence.EventRepositoryAdapter;
import com.ing.assesment.infra.event.persistence.repository.EventJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventInfraConfig {

    @Bean
    public EventRepositoryPort eventRepositoryPort(EventJpaRepository eventJpaRepository) {
        return new EventRepositoryAdapter(eventJpaRepository);
    }
}
