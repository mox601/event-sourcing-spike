package fm.mox.eventsourcingspike;

import static fm.mox.eventsourcingspike.adapter.persistence.MongoDatabaseUtils.printAllEvents;
import static fm.mox.eventsourcingspike.adapter.persistence.MongoDatabaseUtils.printCollectionNames;
import static fm.mox.eventsourcingspike.projection.OrderStatusProjectionUpdater.PROJECTION_ID;
import static fm.mox.eventsourcingspike.view.OrdersCountViewUpdater.VIEW_ID;
import static java.util.Arrays.asList;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsSerDe;
import fm.mox.eventsourcingspike.adapter.persistence.ObjectMapperFactory;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.Event;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.MongoEventRepository;
import fm.mox.eventsourcingspike.domain.OrderPlaced;
import fm.mox.eventsourcingspike.projection.OrdersCountProjection;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.ChangeStreamConsumerState;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.ChangeStreamConsumerStateRepository;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.OrderStatus;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.OrderStatusRepository;
import fm.mox.eventsourcingspike.view.persistence.mongodb.OrdersCount;
import fm.mox.eventsourcingspike.view.persistence.mongodb.OrdersCountRepository;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Testcontainers
//@DataMongoTest
@Slf4j
class EventSourcingSpikeApplicationThreeTests {

    @Container
    static MongoDBContainer MONGODB_CONTAINER =
            new MongoDBContainer(DockerImageName.parse("mongo:6.0.2"));
    private DomainEventsSerDe domainEventsSerDe;
    private MongoDatabase testDatabase;
    private MongoCollection<Event> events;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGODB_CONTAINER::getReplicaSetUrl);
    }

    @Autowired
    private MongoEventRepository mongoEventRepository;

    @Autowired
    private ChangeStreamConsumerStateRepository changeStreamConsumerStateRepository;

    @Autowired
    private OrdersCountRepository ordersCountRepository;
    @Autowired
    private OrderStatusRepository orderStatusRepository;

    @Autowired
    private MongoClient mongoClient;

    @BeforeEach
    public void setUp() {
        log.info("before deleting all");

        this.testDatabase = this.mongoClient.getDatabase("test");
        this.events = this.testDatabase.getCollection("event", Event.class);

        this.mongoEventRepository.deleteAll();
        this.changeStreamConsumerStateRepository.deleteAll();
        this.ordersCountRepository.deleteAll();
        this.orderStatusRepository.deleteAll();

        this.domainEventsSerDe = new DomainEventsSerDe(ObjectMapperFactory.build());
    }

    @AfterEach
    public void tearDown() {
        this.mongoClient.close();
        MONGODB_CONTAINER.close();
    }

    @Test
    void contextLoadsEvent() throws Exception {

        log.info("Events found with findAll(): - should be empty");
        log.info("-------------------------------");
        for (Event event : this.mongoEventRepository.findAll()) {
            log.info(event.toString());
        }
        log.info("-------------------------------");

        // TODO
        // save a couple of events related to 2 distinct entities
        // previous eventsCount is 0 because they are new entities, and we expect no other events on the db
        // expectedPreviousVersion = null to handle optimistic lock: we expect no other events on the db with the same id

        List<String> serializedDomainEventsOne = asList(buildAndSerializeDomainEvent("1"), buildAndSerializeDomainEvent("2"));
        Event one = Event.build("a", "1", serializedDomainEventsOne, 0L);

        List<String> serializedDomainEventsTwo = asList(buildAndSerializeDomainEvent("3"), buildAndSerializeDomainEvent("4"));
        Event two = Event.build("a", "2", serializedDomainEventsTwo, 0L);

        printCollectionNames(this.testDatabase);
        printAllEvents(this.events);

        MongoCollection<OrderStatus> domainEntityItems =
                this.testDatabase.getCollection("domainEntityItem", OrderStatus.class);

        OrdersCountProjection ordersCountProjection =
                new OrdersCountProjection(this.events,
                                          domainEntityItems,
                                          this.orderStatusRepository,
                                          this.ordersCountRepository,
                                          this.changeStreamConsumerStateRepository,
                                          this.domainEventsSerDe);

        ordersCountProjection.start();

        Thread.sleep(5_000L);

        //TODO after every save, print out the status of the intermediate projection table and the view table

        log.info("%%% projection started - no save happened yet %%%");

        this.mongoEventRepository.save(one);

        log.info("%%% after save #1 - sleeping a bit... %%%");

        Thread.sleep(1_000L);

        printAllCollections();

        Thread.sleep(5_000L);

        this.mongoEventRepository.save(two);

        log.info("%%% after save #1 - sleeping a bit... %%%");

        Thread.sleep(1_000L);

        printAllCollections();

        Thread.sleep(1_000L);

        ordersCountProjection.close();
    }

    private void printAllCollections() {
        printCollectionNames(this.testDatabase);
        printAllEvents(this.events);
        printState(PROJECTION_ID);
        printView(VIEW_ID);
    }

    private String buildAndSerializeDomainEvent(String id) {
        return this.domainEventsSerDe.serialize(new OrderPlaced(id, "abc"));
    }

    private void printState(String projectionId) {
        ChangeStreamConsumerState currentChangeStreamConsumerState = this.changeStreamConsumerStateRepository.findById(projectionId).orElse(
                ChangeStreamConsumerState.NULL);
        log.info("projection state: " + currentChangeStreamConsumerState);
    }

    private void printView(String viewId) {
        OrdersCount byId = this.ordersCountRepository.findById(viewId).orElse(OrdersCount.NULL);
        log.info("view state: " + byId);
    }

}
