package fm.mox.eventsourcingspike.adapter.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoEventRepository extends MongoRepository<Event, String> {

    List<Event> findByEntityIdAndEntityType(String entityId, String entityType);

    List<Event> findByEntityType(String entityType);

}