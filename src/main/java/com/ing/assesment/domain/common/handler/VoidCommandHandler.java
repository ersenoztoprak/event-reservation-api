package com.ing.assesment.domain.common.handler;

public interface VoidCommandHandler<TCommand> {
    void handle(TCommand command);
}
