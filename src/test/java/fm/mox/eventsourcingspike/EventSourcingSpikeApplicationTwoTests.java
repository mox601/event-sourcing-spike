package fm.mox.eventsourcingspike;

import static java.util.Arrays.asList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

import fm.mox.eventsourcingspike.adapter.persistence.mongodb.Event;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.MongoEventRepository;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Testcontainers
//@DataMongoTest
@Slf4j
class EventSourcingSpikeApplicationTwoTests {

    @Container
    static MongoDBContainer MONGODB_CONTAINER =
            new MongoDBContainer(DockerImageName.parse("mongo:6.0.2"));

    //private String replicaSetUrl;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGODB_CONTAINER::getReplicaSetUrl);
    }

    @Autowired
    private MongoEventRepository mongoEventRepository;

    @Autowired
    private MongoClient mongoClient;

    @BeforeEach
    public void setUp() throws InterruptedException {
        log.info("heyyy");
        String replicaSetUrl = MONGODB_CONTAINER.getReplicaSetUrl();
        mongoEventRepository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        this.mongoClient.close();
        MONGODB_CONTAINER.close();
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

        Event one = Event.build("a", "1", asList("1", "2"), 0L);
        Event two = Event.build("a", "2", asList("3", "4"), 0L);

        MongoDatabase test = this.mongoClient.getDatabase("test");
        printCollectionNames(test);
        printEvents(test);

        MongoCollection<Event> events = test.getCollection("event", Event.class);

        ChangeStreamIterable<Event> watch = events.watch();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            watch.forEach(x -> {
                log.info("changeStreamDocument: " + x);
            });
        });

        Thread.sleep(5_000L);

        mongoEventRepository.save(one);

        printCollectionNames(test);
        printEvents(test);

        Thread.sleep(5_000L);

        mongoEventRepository.save(two);
        printEvents(test);

        Thread.sleep(1_000L);
        executorService.shutdownNow();

    }

    private void printEvents(MongoDatabase test) {
        MongoCollection<Event> events = test.getCollection("event", Event.class);
        events.find().forEach(printEvent());
    }

    private void printCollectionNames(MongoDatabase test) {
        MongoIterable<String> collectionNames = test.listCollectionNames();
        for (String string : collectionNames) {
            log.info("collection: " + string);
        }
    }

    private Consumer<Event> printEvent() {
        return x -> log.info("event: " + x);
    }

}
