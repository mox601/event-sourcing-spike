package fm.mox.eventsourcingspike.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fm.mox.eventsourcingspike.domain.ADomainEntityCreated;

public class DomainEventsSerDe {
    private final ObjectMapper objectMapper;

    public DomainEventsSerDe(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(ADomainEntityCreated domainEvent) {
        try {
            return this.objectMapper.writeValueAsString(domainEvent);
        } catch (JsonProcessingException e) {
            //TODO throw new RuntimeException(e);
            return null;
        }
    }

    public ADomainEntityCreated deserialize(String serializedDomainEvent) {
        ADomainEntityCreated domainEvent = null;
        try {
            //https://github.com/sigpwned/jackson-modules-java-17-sealed-classes
            // TODO https://web.archive.org/web/20200623023452/https://programmerbruce.blogspot.com/2011/05/deserialize-json-with-jackson-into.html
            domainEvent = this.objectMapper.readValue(serializedDomainEvent, ADomainEntityCreated.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return domainEvent;
    }
}
