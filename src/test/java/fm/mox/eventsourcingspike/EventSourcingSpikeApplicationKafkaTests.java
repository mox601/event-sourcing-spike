package fm.mox.eventsourcingspike;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import fm.mox.eventsourcingspike.adapter.persistence.InMemoryDomainEventsPersistenceAdapter;
import fm.mox.eventsourcingspike.domain.DomainEvent;
import fm.mox.eventsourcingspike.orders.adapter.persistence.OrderRepository;
import fm.mox.eventsourcingspike.orders.commands.PlaceOrder;
import fm.mox.eventsourcingspike.orders.commands.handlers.KafkaPlaceOrderSubscriber;
import fm.mox.eventsourcingspike.orders.commands.handlers.PlaceOrderHandler;
import fm.mox.eventsourcingspike.orders.domain.OrderFactory;
import fm.mox.eventsourcingspike.orders.services.PlaceOrderService;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Testcontainers
//@DataMongoTest
@Slf4j
class EventSourcingSpikeApplicationKafkaTests {

    @Container
    static MongoDBContainer MONGODB_CONTAINER =
            new MongoDBContainer(DockerImageName.parse("mongo:6.0.2"));

    // use it as command bus
    @Container
    static KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"));

    //private KafkaConsumer<String, DataRecord> consumer;
    //https://docs.confluent.io/platform/current/installation/versions-interoperability.html#cp-and-apache-kafka-compatibility
    private KafkaPlaceOrderSubscriber kafkaPlaceOrderSubscriber;
    private PlaceOrderService placeOrderService;
    private Map<String, Map<Long, List<DomainEvent>>> map;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGODB_CONTAINER::getReplicaSetUrl);
        log.info("kafka " + KAFKA.getBootstrapServers());
    }

    @BeforeEach
    public void setUp() throws Exception {

        String placeOrderTopic = "place-order-topic";

        //create topic
        Properties topicCreatorProps = new Properties();
        topicCreatorProps.put("bootstrap.servers", KAFKA.getBootstrapServers());
        topicCreatorProps.put("schema.registry.url", "http://localhost:8081");

        createTopic(placeOrderTopic, topicCreatorProps);

        //create producer
        Producer<String, PlaceOrder> producer = PlaceOrderService.createProducer(KAFKA.getBootstrapServers(), "http://localhost:8081");

        this.placeOrderService = new PlaceOrderService(producer, placeOrderTopic);

        // create consumer
        KafkaConsumer<String, PlaceOrder> stringPlaceOrderKafkaConsumer = KafkaPlaceOrderSubscriber.buildConsumer(
                KAFKA.getBootstrapServers(), "http://localhost:8081");

        //create place order consumer
        this.map = new HashMap<>();
        OrderRepository orderRepository = new OrderRepository(
                new InMemoryDomainEventsPersistenceAdapter(this.map),
                new OrderFactory()
        );
        PlaceOrderHandler placeOrderCommandHandler = new PlaceOrderHandler(orderRepository);
        this.kafkaPlaceOrderSubscriber = new KafkaPlaceOrderSubscriber(stringPlaceOrderKafkaConsumer,
                                                                       placeOrderCommandHandler,
                                                                       placeOrderTopic);
    }

    public static void createTopic(final String topic,
                                   final Properties props) {
        //TODO how many partitions?
        Optional<Integer> numPartitions = Optional.empty();
        Optional<Short> replicationFactor = Optional.empty();
        final NewTopic newTopic = new NewTopic(topic, numPartitions, replicationFactor);
        try (final AdminClient adminClient = AdminClient.create(props)) {
            adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
        } catch (final InterruptedException | ExecutionException e) {
            // Ignore if TopicExistsException, which may be valid if topic exists
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new RuntimeException(e);
            }
        }
    }

    @AfterEach
    public void tearDown() {
        KAFKA.close();
        MONGODB_CONTAINER.stop();
    }

    @Test
    void contextLoadsEvent1() throws Exception {

        //start consumer
        Thread consumerThread = new Thread(() -> this.kafkaPlaceOrderSubscriber.start());
        consumerThread.start();

        Thread.sleep(2_000L);

        // Produce sample data
        final long numMessages = 10L;
        for (long i = 0L; i < numMessages; i++) {
            this.placeOrderService.placeOrder(String.valueOf(i));
            Thread.sleep(1_000L);
        }

        this.placeOrderService.close();

        String msg = String.format("10 messages were produced to topic %s%n", "kafka-topic");
        log.info(msg);

        this.kafkaPlaceOrderSubscriber.stop();

        consumerThread.join();

        log.info(map.toString());
    }
}
