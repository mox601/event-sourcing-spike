package fm.mox.eventsourcingspike.common;

import fm.mox.eventsourcingspike.domain.ADomainEntityCreated;

import java.util.List;

public interface DomainEntityInterface {

    String getId();

    Long getVersion();

    List<ADomainEntityCreated> getEvents();

}
