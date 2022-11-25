package fm.mox.eventsourcingspike.services;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import fm.mox.eventsourcingspike.commands.PlaceOrder;
import lombok.extern.slf4j.Slf4j;

//this could be a service
@Slf4j
public class PlaceOrderService {

    private final Producer<String, PlaceOrder> producer;
    private final String topic;

    public PlaceOrderService(Producer<String, PlaceOrder> producer, String placeOrderTopic) {
        this.producer = producer;
        this.topic = placeOrderTopic;
    }

    public void placeOrder(String placeOrderId) {
        String key = "alice"; // TODO what key to use?
        PlaceOrder record = new PlaceOrder("id-" + placeOrderId);
        //String record = objectMapper.writeValueAsString(value);
        log.info("Producing record: " + key + " " + record + "");

        this.producer.send(new ProducerRecord<>(topic, key, record), (m, e) -> {
            if (e != null) {
                e.printStackTrace();
            } else {
                String msg = String.format("Produced record to topic %s partition [%d] @ offset %d%n",
                                           m.topic(), m.partition(), m.offset());
                log.info(msg);
            }
        });
        //TODO return a command id or an ack the command was published successfully
    }

    public static KafkaProducer<String, PlaceOrder> createProducer(String bootstrapServers, String value) {
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", bootstrapServers);
        producerProps.put("schema.registry.url", value);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaJsonSerializer");
        return new KafkaProducer<>(producerProps);
    }

    public void close() {
        this.producer.flush();
        this.producer.close();
    }
}
