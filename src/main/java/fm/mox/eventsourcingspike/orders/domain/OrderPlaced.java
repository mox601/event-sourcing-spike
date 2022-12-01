package fm.mox.eventsourcingspike.orders.domain;

import fm.mox.eventsourcingspike.domain.DomainEvent;
import lombok.Value;

//TODO sealed interface?
@Value
public class OrderPlaced implements DomainEvent {

    String entityId;

    String status;

}
