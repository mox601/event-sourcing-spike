package fm.mox.eventsourcingspike.domain;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

// TODO can it provide a couple of default fields?
@JsonTypeInfo(use = NAME, include = PROPERTY)
public interface DomainEvent {

}
