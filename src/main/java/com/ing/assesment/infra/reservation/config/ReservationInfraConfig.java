package com.ing.assesment.infra.reservation.config;

import com.ing.assesment.domain.idempotency.port.IdempotencyKeyRepositoryPort;
import com.ing.assesment.domain.idempotency.port.RequestHashPort;
import com.ing.assesment.domain.reservation.port.ReservationRepositoryPort;
import com.ing.assesment.infra.idempotency.hash.Sha256RequestHashAdapter;
import com.ing.assesment.infra.idempotency.persistence.adapter.IdempotencyKeyRepositoryAdapter;
import com.ing.assesment.infra.idempotency.persistence.repository.IdempotencyKeyJpaRepository;
import com.ing.assesment.infra.reservation.persistence.adapter.ReservationRepositoryAdapter;
import com.ing.assesment.infra.reservation.persistence.repository.ReservationJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReservationInfraConfig {

    @Bean
    public ReservationRepositoryPort reservationRepositoryPort(ReservationJpaRepository reservationJpaRepository) {
        return new ReservationRepositoryAdapter(reservationJpaRepository);
    }

    @Bean
    public IdempotencyKeyRepositoryPort idempotencyKeyRepositoryPort(IdempotencyKeyJpaRepository repository) {
        return new IdempotencyKeyRepositoryAdapter(repository);
    }

    @Bean
    public RequestHashPort requestHashPort() {
        return new Sha256RequestHashAdapter();
    }
}
