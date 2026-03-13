package com.ing.assesment.infra.event.api.request;

import jakarta.validation.constraints.NotNull;

public record PublishEventRequest(
        @NotNull Long version) {
}
