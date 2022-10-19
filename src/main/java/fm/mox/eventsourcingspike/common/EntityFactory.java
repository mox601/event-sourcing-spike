package fm.mox.eventsourcingspike.common;

import fm.mox.eventsourcingspike.domain.ADomainEntityCreated;

import java.util.List;

public interface EntityFactory<T> {
    T from(List<ADomainEntityCreated> events);
}
