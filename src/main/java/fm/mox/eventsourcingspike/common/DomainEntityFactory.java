package fm.mox.eventsourcingspike.common;

import java.util.List;

/**
 * @param <T> domain class
 * @param <E> domain events base class
 */
public interface DomainEntityFactory<T, E> {

    T from(List<E> events);
}
