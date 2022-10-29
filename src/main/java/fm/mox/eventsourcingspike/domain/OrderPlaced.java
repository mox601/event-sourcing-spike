package fm.mox.eventsourcingspike.domain;

import lombok.Value;

//TODO sealed interface?
@Value
public class OrderPlaced implements DomainEvent {

    String entityId;

    String status;

}
