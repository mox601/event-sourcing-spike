package fm.mox.eventsourcingspike.domain;

import lombok.Value;

//sealed interface?
@Value
public class ADomainEntityCreated {
    String entityId;
}
