package fm.mox.eventsourcingspike;

import com.fasterxml.jackson.databind.ObjectMapper;
import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsPersistenceAdapter;
import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsPersistenceAdapterImpl;
import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsSerDe;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.MongoEventRepository;
import fm.mox.eventsourcingspike.domain.ADomainEntity;
import fm.mox.eventsourcingspike.domain.ADomainEntityCreated;
import fm.mox.eventsourcingspike.domain.ADomainEntityFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class DomainApplicationTests {

    @Autowired
    private MongoEventRepository mongoEventRepository;

    @Test
    void contextLoadsEvent() {

        this.mongoEventRepository.deleteAll();

        //TODO bimap from class to entityType
        String aDomainEntityType = "a-domain-entity";
        Map<String, Class<?>> stringClassHashMap = new HashMap<>();
        stringClassHashMap.put(aDomainEntityType, ADomainEntity.class);

        //TODO reserve an id from a collection

        ADomainEntity aDomainEntity = new ADomainEntity();
        aDomainEntity.handle(new ADomainEntityCreated("1"));

        ObjectMapper objectMapper = new ObjectMapper();
        DomainEventsSerDe domainEventsSerDe = new DomainEventsSerDe(objectMapper);
        DomainEventsPersistenceAdapter domainEventsPersistenceAdapter = new DomainEventsPersistenceAdapterImpl(
                this.mongoEventRepository,
                domainEventsSerDe
        );

        //save domain events, get the id of the document stored on db

        String savedEventId = domainEventsPersistenceAdapter.save(
                aDomainEntityType,
                aDomainEntity.getId(),
                aDomainEntity.getEvents(),
                aDomainEntity.getVersion());

        System.out.println(savedEventId);

        //read
        List<ADomainEntityCreated> domainEvents = domainEventsPersistenceAdapter.findById(
                aDomainEntityType,
                "1");
        System.out.println(domainEvents);

        ADomainEntity fromEvents = new ADomainEntityFactory().from(domainEvents);

        // TODO implement a projection (build a view) check idempotency when consuming changes
        // https://github.com/mongodb-developer/java-quick-start
        // TODO implement a process manager
        // TODO implement snapshotting
    }

    //domain event repository


}
