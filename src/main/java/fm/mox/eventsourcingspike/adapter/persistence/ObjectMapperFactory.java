package fm.mox.eventsourcingspike.adapter.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;

import fm.mox.eventsourcingspike.domain.OrderPlaced;

public final class ObjectMapperFactory {

    private ObjectMapperFactory() {
        //
    }

    public static ObjectMapper build() {
        ObjectMapper objectMapper = new ObjectMapper();
        //one per event type
        objectMapper.registerSubtypes(new NamedType(OrderPlaced.class, "a-domain-entity-created"));
        return objectMapper;

    }
}
