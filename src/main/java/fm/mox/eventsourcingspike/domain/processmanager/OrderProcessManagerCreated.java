package fm.mox.eventsourcingspike.domain.processmanager;

import fm.mox.eventsourcingspike.domain.DomainEvent;
import lombok.Value;

@Value
public class OrderProcessManagerCreated implements DomainEvent {

    String entityId;

}
