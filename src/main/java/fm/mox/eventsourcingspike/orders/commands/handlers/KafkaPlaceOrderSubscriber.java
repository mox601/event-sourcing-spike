package fm.mox.eventsourcingspike.orders.commands.handlers;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import fm.mox.eventsourcingspike.common.CommandHandler;
import fm.mox.eventsourcingspike.orders.commands.PlaceOrder;
import io.confluent.kafka.serializers.KafkaJsonDeserializerConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaPlaceOrderSubscriber {

    private final KafkaConsumer<String, PlaceOrder> consumer;

    private final String topic;

    private final CommandHandler<PlaceOrder> placeOrderCommandHandler;
    private volatile boolean isRunning;

    public KafkaPlaceOrderSubscriber(KafkaConsumer<String, PlaceOrder> consumer,
                                     CommandHandler<PlaceOrder> placeOrderCommandHandler,
                                     String topic) {
        this.consumer = consumer;
        //TODO could it manage and notify multiple handlers for the same command?
        this.placeOrderCommandHandler = placeOrderCommandHandler;
        this.topic = topic;
        this.isRunning = true;
    }

    public void start() {
        log.info("started");

        this.consumer.subscribe(List.of(this.topic));

        try {
            while (this.isRunning) {
                ConsumerRecords<String, PlaceOrder> records = this.consumer.poll(Duration.ofMillis(100));
                log.info("polled " + records.count());
                if (records.count() > 0) {
                    for (ConsumerRecord<String, PlaceOrder> record : records) {
                        String key = record.key();
                        PlaceOrder value = record.value();
                        //TODO deduplicate?
                        //consume record
                        try {
                            this.placeOrderCommandHandler.handle(value);
                        } catch (Exception e) {
                            //TODO how to deal with exceptions?
                        }
                        //TODO save bookmark? or is it handled by kafka?
                        //record.offset();
                    }
                }
            }
        } finally {
            this.consumer.close();
        }
        log.info("stopped");
    }

    public void stop() {
        this.isRunning = false;
    }

    public static KafkaConsumer<String, PlaceOrder> buildConsumer(String bootstrapServers,
                                                                  String schemaRegistryUrl) {
        final Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", bootstrapServers);
        consumerProps.put("schema.registry.url", schemaRegistryUrl);
        // Add additional properties.
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaJsonDeserializer");
        consumerProps.put(KafkaJsonDeserializerConfig.JSON_VALUE_TYPE, PlaceOrder.class);
        //TODO how many consumers can we have?
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "place-order-consumer-1");
        //TODO should it start from earliest? or latest?
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(consumerProps);
    }

}
