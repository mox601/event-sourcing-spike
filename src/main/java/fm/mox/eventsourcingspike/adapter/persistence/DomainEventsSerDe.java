package fm.mox.eventsourcingspike.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fm.mox.eventsourcingspike.domain.DomainEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DomainEventsSerDe {

    private final ObjectMapper objectMapper;

    public DomainEventsSerDe(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(DomainEvent domainEvent) {
        try {
            return this.objectMapper.writeValueAsString(domainEvent);
        } catch (JsonProcessingException e) {
            log.error("error while serializing '" + domainEvent + "'", e);
            //TODO throw new RuntimeException(e);
            return null;
        }
    }

    public DomainEvent deserialize(String serializedDomainEvent) {
        DomainEvent domainEvent = null;
        try {
            domainEvent = this.objectMapper.readValue(serializedDomainEvent, DomainEvent.class);
        } catch (JsonProcessingException e) {
            log.error("error while deserializing '" + serializedDomainEvent + "'", e);
        }
        return domainEvent;
    }
}
