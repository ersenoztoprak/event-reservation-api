package com.ing.assesment.infra.event.config;

import com.ing.assesment.domain.auth.port.CurrentUserPort;
import com.ing.assesment.domain.event.command.handler.CreateEventCommandHandler;
import com.ing.assesment.domain.event.command.handler.PublishEventCommandHandler;
import com.ing.assesment.domain.event.command.handler.UpdateEventCommandHandler;
import com.ing.assesment.domain.event.port.EventRepositoryPort;
import com.ing.assesment.domain.event.query.handler.ListEventsQueryHandler;
import com.ing.assesment.domain.event.query.handler.SearchPublicEventsQueryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventBeanConfig {

    @Bean
    public CreateEventCommandHandler createEventCommandHandler(EventRepositoryPort eventRepositoryPort,
                                                               CurrentUserPort currentUserPort) {
        return new CreateEventCommandHandler(eventRepositoryPort, currentUserPort);
    }

    @Bean
    public UpdateEventCommandHandler updateEventCommandHandler(EventRepositoryPort eventRepositoryPort,
                                                               CurrentUserPort currentUserPort) {
        return new UpdateEventCommandHandler(eventRepositoryPort, currentUserPort);
    }

    @Bean
    public PublishEventCommandHandler publishEventCommandHandler(EventRepositoryPort eventRepositoryPort,
                                                                 CurrentUserPort currentUserPort) {
        return new PublishEventCommandHandler(eventRepositoryPort, currentUserPort);
    }

    @Bean
    public ListEventsQueryHandler listEventsQueryHandler(EventRepositoryPort eventRepositoryPort,
                                                         CurrentUserPort currentUserPort) {
        return new ListEventsQueryHandler(eventRepositoryPort, currentUserPort);
    }

    @Bean
    public SearchPublicEventsQueryHandler searchPublicEventsQueryHandler(EventRepositoryPort eventRepositoryPort) {
        return new SearchPublicEventsQueryHandler(eventRepositoryPort);
    }
}
