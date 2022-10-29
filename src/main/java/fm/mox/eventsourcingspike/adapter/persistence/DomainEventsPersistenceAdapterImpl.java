package fm.mox.eventsourcingspike.adapter.persistence;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fm.mox.eventsourcingspike.adapter.persistence.mongodb.Event;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.MongoEventRepository;
import fm.mox.eventsourcingspike.domain.DomainEvent;

public class DomainEventsPersistenceAdapterImpl implements DomainEventsPersistenceAdapter {

    private final MongoEventRepository mongoEventRepository;
    private final DomainEventsSerDe domainEventsSerDe;

    public DomainEventsPersistenceAdapterImpl(MongoEventRepository mongoEventRepository,
                                              DomainEventsSerDe domainEventsSerDe) {
        this.mongoEventRepository = mongoEventRepository;
        this.domainEventsSerDe = domainEventsSerDe;
    }

    @Override
    public List<DomainEvent> findById(String entityType,
                                      String entityId) {
        return this.mongoEventRepository.findByEntityIdAndEntityType(entityId, entityType)
                .stream()
                .flatMap((Function<Event, Stream<String>>) event -> event.getSerializedDomainEvents().stream())
                .map(this.domainEventsSerDe::deserialize)
                .collect(Collectors.toList());
    }

    @Override
    public String save(String entityType,
                       String entityId,
                       List<DomainEvent> events,
                       Long version) {
        List<String> serializedDomainEvents = events.stream()
                .map(this.domainEventsSerDe::serialize)
                .collect(Collectors.toList());
        Event event = Event.build(entityType, entityId, serializedDomainEvents, version);
        return this.mongoEventRepository.save(event).getId();
    }
}
