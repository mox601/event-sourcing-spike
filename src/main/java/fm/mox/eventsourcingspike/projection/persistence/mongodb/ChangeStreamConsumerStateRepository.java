package fm.mox.eventsourcingspike.projection.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChangeStreamConsumerStateRepository extends MongoRepository<ChangeStreamConsumerState, String> {

}
