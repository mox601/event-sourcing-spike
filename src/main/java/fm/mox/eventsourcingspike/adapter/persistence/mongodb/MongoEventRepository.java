package fm.mox.eventsourcingspike.adapter.persistence.mongodb;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoEventRepository extends MongoRepository<Event, String> {

    List<Event> findByEntityIdAndEntityType(String entityId, String entityType);

    List<Event> findByEntityType(String entityType);

}
