package fm.mox.eventsourcingspike.billing.domain;

import fm.mox.eventsourcingspike.domain.DomainEvent;
import lombok.Value;

//TODO sealed interface?
@Value
public class BillCreated implements DomainEvent {

    String entityId;

    String status;

}
