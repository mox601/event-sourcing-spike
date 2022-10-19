package fm.mox.eventsourcingspike.adapter.persistence;

import fm.mox.eventsourcingspike.domain.ADomainEntityCreated;

import java.util.List;

public interface DomainEventsPersistenceAdapter {

    List<ADomainEntityCreated> findById(String entityType, String entityId);

    // TODO what is best to return?
    String save(String entityType, String id, List<ADomainEntityCreated> events, Long version);

}
