package fm.mox.eventsourcingspike.common;

import java.util.List;

import fm.mox.eventsourcingspike.domain.DomainEvent;

/**
 *
 */
public interface DomainEntity {

    String getId();

    Long getVersion();

    List<DomainEvent> getUncommittedEvents();

}
