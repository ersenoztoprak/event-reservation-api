package com.ing.assesment.domain.common;

public interface CommandHandler<TCommand, TResult> {
    TResult handle(TCommand command);
}
