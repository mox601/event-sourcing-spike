package fm.mox.eventsourcingspike.adapter.persistence;

import java.util.List;

import fm.mox.eventsourcingspike.domain.DomainEvent;

public interface DomainEventsPersistenceAdapter {

    List<DomainEvent> findById(String entityType, String entityId);

    // TODO what is best to return?
    String save(String entityType, String id, List<DomainEvent> events, Long version);

}
