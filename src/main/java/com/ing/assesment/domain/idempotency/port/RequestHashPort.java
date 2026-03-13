package com.ing.assesment.domain.idempotency.port;

public interface RequestHashPort {

    String hash(String raw);
}
