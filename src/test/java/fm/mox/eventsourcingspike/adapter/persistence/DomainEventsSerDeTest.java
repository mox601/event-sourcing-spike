package fm.mox.eventsourcingspike.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fm.mox.eventsourcingspike.orders.domain.OrderPlaced;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DomainEventsSerDeTest {

    private DomainEventsSerDe underTest;
    private ObjectMapper forAssertions;

    @BeforeEach
    void setUp() {
        this.forAssertions = new ObjectMapper();
        this.underTest = new DomainEventsSerDe(ObjectMapperFactory.build());
    }

    @Test
    void canSerializeAndDeserializeADomainEntityCreated() throws Exception {
        assertSerializingAndDeserializing(new OrderPlaced("abc", "some status"), "a-domain-entity-created");
    }

    @Test
    void canSerializeAndDeserializeAnotherDomainEvent() throws Exception {
        //TODO
        assertSerializingAndDeserializing(new OrderPlaced("abc", "some status"), "a-domain-entity-created");
    }

    private void assertSerializingAndDeserializing(OrderPlaced input, String expectedType) throws Exception {
        String serialized = this.underTest.serialize(input);
        log.info(serialized);
        assertTypeIsExpected(expectedType, serialized);
        OrderPlaced deserialized = (OrderPlaced) this.underTest.deserialize(serialized);
        assertEquals(input, deserialized);
    }

    private void assertTypeIsExpected(String expectedType, String serialized) throws Exception {
        JsonNode jsonNode = this.forAssertions.readTree(serialized);
        String actual = jsonNode.get("@type").asText();
        assertEquals(expectedType, actual);
    }
}
