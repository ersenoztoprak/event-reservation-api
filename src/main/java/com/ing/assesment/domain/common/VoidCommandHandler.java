package com.ing.assesment.domain.common;

public interface VoidCommandHandler<TCommand> {
    void handle(TCommand command);
}
