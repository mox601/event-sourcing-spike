package fm.mox.eventsourcingspike.adapter.persistence.mongodb;

import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.util.List;

@Value
public class Event {

    /**
     * concat N fields:
     * - (if we use a collection in mongodb, no need for it) entityType
     * - entityId
     * - previousEventsCount
     */
    @Id
    String id;

    /**
     * the amount of Events we expect to be previously stored on the db
     * for this same entityType and entityId.
     * It can be the same as the domain entity version,
     * if the version is incremented every time a command is applied
     * (regardless of the generated event count)
     */
    Long previousEventsCount;

    //TODO can be inferred from collection name? how to store this same class in different collections?
    //probably needs lower level mongodb APIs
    String entityType;
    /**
     * domain entity id
     */
    String entityId;

    /**
     * used for optimistic concurrency
     * expectedPreviousVersion = null to handle optimistic lock:
     * we expect no other events on the db with the same id
     * can be always set as null in the public factory method
     * so that save() will trigger the concurrency check
     */
    @Version
    Long expectedPreviousVersion;

    /**
     * domain events returned by handling a single domain command are json serialized and stored together
     */
    List<String> serializedDomainEvents;

    /**
     * TODO how to provide a total ordering on all domain events?
     *  currently, they are stored in the same collection, so it's ok.
     * <p>
     * But what if each aggregate had it's own collection?
     * <p>
     * options:
     * <p>
     * 1. create 1 collection per aggregate and add a field systemEventCount
     * every time a new event is persisted, the next system event counter is reserved changing a collection with an
     * incremental counter like...
     * <p>
     * But what if a subscriber wants to subscribe to $all events?
     * it has to get the list of all existing streams, and update them as they are created during its lifecycle,
     * otherwise it could lose the streams created since the start of his subscription.
     * Could it get the stream of created new streams?
     * Who is in charge of creating that?
     * <p>
     * 2. keep events in 1 single collection
     * then, implement a projection that writes 1 stream per entity.
     * The subscriber that subscribes to $all events subscribes to the $all collection.
     * When loading 1 aggregate, we could get a not up to date list of events because the 1 stream per entity writer
     * could be lagging behind.
     */

    public static Event build(String entityType,
                              String entityId,
                              List<String> serializedDomainEvents,
                              Long previousEventsCount) {
        return new Event(entityType + "-" + entityId + "-" + previousEventsCount,
                previousEventsCount,
                entityType,
                entityId,
                null,
                serializedDomainEvents);
    }
}
