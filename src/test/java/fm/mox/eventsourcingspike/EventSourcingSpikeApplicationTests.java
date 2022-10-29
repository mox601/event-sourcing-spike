package fm.mox.eventsourcingspike;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import fm.mox.eventsourcingspike.adapter.persistence.mongodb.Event;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.MongoEventRepository;
import lombok.extern.slf4j.Slf4j;

//@SpringBootTest
@Testcontainers
@DataMongoTest
@Slf4j
class EventSourcingSpikeApplicationTests {

    @Container
    static MongoDBContainer MONGODB_CONTAINER =
            new MongoDBContainer(DockerImageName.parse("mongo:6.0.2"));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGODB_CONTAINER::getReplicaSetUrl);
    }

    @Autowired
    private MongoEventRepository mongoEventRepository;

    @BeforeEach
    public void setUp() throws InterruptedException {
        mongoEventRepository.deleteAll();
    }

    @Test
    void contextLoadsEvent() throws InterruptedException {

        log.info("Events found with findAll():");
        log.info("-------------------------------");
        for (Event event : mongoEventRepository.findAll()) {
            log.info(event.toString());
        }

        // save a couple of events related to 2 distinct entities
        // previous eventsCount is 0 because they are new entities, and we expect no other events on the db
        // expectedPreviousVersion = null to handle optimistic lock: we expect no other events on the db with the same id

        Event one = Event.build("a", "1", Arrays.asList("1", "2"), 0L);
        Event two = Event.build("a", "2", Arrays.asList("3", "4"), 0L);

        mongoEventRepository.save(one);
        mongoEventRepository.save(two);

        // fetch all customers
        log.info("Events found with findAll():");
        log.info("-------------------------------");
        for (Event event : mongoEventRepository.findAll()) {
            log.info(event.toString());
        }

        // fetch by entity type
        log.info("Events found with findByEntityType:");
        log.info("--------------------------------");
        log.info(mongoEventRepository.findByEntityType("a").toString());

        log.info("Event found with findById('a-1-0'):");
        log.info("--------------------------------");
        log.info(mongoEventRepository.findById("a-1-0").toString());

        Event three = Event.build("a", "3", Arrays.asList("3", "4"), 0L);
        mongoEventRepository.save(three);

        Event oneBis = Event.build("a", "3", Arrays.asList("5", "6"), 1L);

        //TODO this fails!
        //mongoEventRepository.save(oneBis);

        //expectedPreviousVersion = null doesn't work
        // org.springframework.dao.DuplicateKeyException: Write operation error on server localhost:65019. Write error: WriteError{code=11000, message='E11000 duplicate key error collection: test.event index: _id_ dup key: { _id: "1-0" }', details={}}.; nested exception is com.mongodb.MongoWriteException: Write operation error on server localhost:65019. Write error: WriteError{code=11000, message='E11000 duplicate key error collection: test.event index: _id_ dup key: { _id: "1-0" }', details={}}.

        //expectedPreviousVersion = 0 works

        //expectedPreviousVersion = 1 doesn't work:
        // org.springframework.dao.OptimisticLockingFailureException: Cannot save entity 1-0 with version 2 to collection event. Has it been modified meanwhile?

        log.info("Event found with findById('a-1-0'):");
        log.info("--------------------------------");
        log.info(mongoEventRepository.findById("a-1-0").toString());
    }

    //domain event repository

}
