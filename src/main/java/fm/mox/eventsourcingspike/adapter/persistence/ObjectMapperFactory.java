package fm.mox.eventsourcingspike.adapter.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;

import fm.mox.eventsourcingspike.orders.domain.OrderPlaced;

public final class ObjectMapperFactory {

    private ObjectMapperFactory() {
        //
    }

    public static ObjectMapper build() {
        ObjectMapper objectMapper = new ObjectMapper();
        //one per event type
        objectMapper.registerSubtypes(new NamedType(OrderPlaced.class, "order-placed"));
        return objectMapper;

    }
}
