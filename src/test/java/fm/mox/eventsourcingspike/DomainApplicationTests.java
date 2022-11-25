package fm.mox.eventsourcingspike;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsPersistenceAdapter;
import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsPersistenceAdapterImpl;
import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsSerDe;
import fm.mox.eventsourcingspike.adapter.persistence.ObjectMapperFactory;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.MongoEventRepository;
import fm.mox.eventsourcingspike.domain.DomainEvent;
import fm.mox.eventsourcingspike.domain.Order;
import fm.mox.eventsourcingspike.domain.OrderFactory;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
class DomainApplicationTests {

    @Autowired
    private MongoEventRepository mongoEventRepository;
    private DomainEventsPersistenceAdapter domainEventsPersistenceAdapter;

    @BeforeEach
    void setUp() {

        DomainEventsSerDe domainEventsSerDe = new DomainEventsSerDe(ObjectMapperFactory.build());
        this.domainEventsPersistenceAdapter = new DomainEventsPersistenceAdapterImpl(
                this.mongoEventRepository,
                domainEventsSerDe
        );
    }

    @Test
    void contextLoadsEvent() {

        this.mongoEventRepository.deleteAll();

        //TODO bimap from class to entityType
        String aDomainEntityType = "a-domain-entity";
        Map<String, Class<?>> stringClassHashMap = new HashMap<>();
        stringClassHashMap.put(aDomainEntityType, Order.class);

        //TODO reserve an id from a collection

        Order order = new Order("1");

        //save domain events, get the id of the document stored on db

        String savedEventId = this.domainEventsPersistenceAdapter.save(
                aDomainEntityType,
                order.getId(),
                order.getUncommittedEvents(),
                order.getVersion());

        log.info(savedEventId);

        //read
        List<DomainEvent> domainEvents = this.domainEventsPersistenceAdapter.findById(
                aDomainEntityType,
                "1");
        log.info(domainEvents.toString());

        Order fromEvents = new OrderFactory().from(domainEvents);

        // TODO implement a process manager
        // TODO implement snapshotting
    }

    //domain event repository

}
