package fm.mox.eventsourcingspike.orders.processmanager;

import fm.mox.eventsourcingspike.domain.DomainEvent;
import lombok.Value;

@Value
public class OrderProcessManagerCreated implements DomainEvent {

    String entityId;

}
