package com.ing.assesment.domain.common.handler;

public interface CommandHandler<TCommand, TResult> {
    TResult handle(TCommand command);
}
