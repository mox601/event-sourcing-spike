package fm.mox.eventsourcingspike.projection;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

import org.bson.BsonDocument;
import org.bson.BsonString;

import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

import fm.mox.eventsourcingspike.OrderProcessManager;
import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsSerDe;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.Event;
import fm.mox.eventsourcingspike.domain.OrderPlaced;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.ChangeStreamConsumerState;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.ChangeStreamConsumerStateRepository;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.OrderStatus;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.OrderStatusRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes the mongod change stream and writes both - to a state - to an intermediate collection that prepares the data for the actual
 * view
 */
@Slf4j
public class OrderPlacedEventListener implements Runnable, Closeable {

    public static final String PROJECTION_ID = "order-placed-event-listener";
    volatile boolean shouldRun;
    private final MongoCollection<Event> events;
    private final DomainEventsSerDe domainEventsSerDe;
    private final ChangeStreamConsumerStateRepository changeStreamConsumerStateRepository;
    private final OrderStatusRepository orderStatusRepository;

    public OrderPlacedEventListener(MongoCollection<Event> events,
                                    DomainEventsSerDe domainEventsSerDe,
                                    ChangeStreamConsumerStateRepository changeStreamConsumerStateRepository,
                                    OrderStatusRepository orderStatusRepository) {
        this.events = events;
        this.domainEventsSerDe = domainEventsSerDe;
        this.changeStreamConsumerStateRepository = changeStreamConsumerStateRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.shouldRun = true;
    }

    @Override
    public void run() {
        //TODO retry if exception
        ChangeStreamConsumerState previousProjectionChangeStreamConsumerState = previousProjectionState();
        log.info("initial consumerstate: " + previousProjectionChangeStreamConsumerState);
        // TODO also deal with the possibility that watch() throws exception,
        //  wrapping in a try-catch and restarting it
        ChangeStreamIterable<Event> watch = watchResumingFrom(previousProjectionChangeStreamConsumerState);

        // doesn't use this.watch.forEach(...); uses iterator so we can better control when to stop
        try (MongoCursor<ChangeStreamDocument<Event>> iterator = watch.iterator()) {
            //TODO deal with the thread being stopped
            while (this.shouldRun && iterator.hasNext()) {
                ChangeStreamDocument<Event> eventChangeStreamDocument = iterator.next();
                logResumeToken(eventChangeStreamDocument);
                log.info("csd: " + eventChangeStreamDocument);
                //extract
                Event fullDocument = eventChangeStreamDocument.getFullDocument();
                //deserialize
                //TODO catch exceptions for deserialization and when writing state
                if (fullDocument != null) {
                    Optional<OrderPlaced> orderPlacedOpt = findFirstOrderPlaced(fullDocument);
                    if (orderPlacedOpt.isPresent()) {
                        OrderPlaced orderPlaced = orderPlacedOpt.get();
                        log.info("processing orderPlaced: " + orderPlaced);

                        // TODO init process manager for handling payments and shipping
                        /*
                        load process manager by order id
                            it's event sourced itself
                        make it handle the orderplaced
                            it will call a command on another aggregate

                        store the process manager events
                         */

                        //TODO load from repository
                        OrderProcessManager orderProcessManager = new OrderProcessManager();
                        orderProcessManager.handle(orderPlaced);
                        //save

                        //OrderStatus orderStatus = new OrderStatus(orderPlaced.getEntityId(), orderPlaced.getStatus());
                        // we assume only 1 thread is consuming the change stream to update this projection
                        // TODO process "idempotently" each message, e.g. writing to a collection to build the view
                        // TODO what happens if it was already there?
                        //this.orderStatusRepository.save(orderStatus);
                    }
                }
                saveResumeTokenAsState(eventChangeStreamDocument);
            }
            log.info("stopped");
        }
        log.info("watch iterator closed");
    }

    private Optional<OrderPlaced> findFirstOrderPlaced(Event fullDocument) {
        return fullDocument
                .getSerializedDomainEvents()
                .stream()
                .map(this.domainEventsSerDe::deserialize)
                .filter(e -> e instanceof OrderPlaced)
                .map(e -> (OrderPlaced) e)
                .findFirst();
    }

    // TODO call it when the app is stopped
    @Override
    public void close() throws IOException {
        log.info("stopping");
        this.shouldRun = false;
    }

    /**
     * TODO decouple from mongodb technology - use an event bus?
     * watch Or Resume After Token
     *
     * @param previousProjectionChangeStreamConsumerState previous state
     * @return a change stream iterable
     */
    private ChangeStreamIterable<Event> watchResumingFrom(ChangeStreamConsumerState previousProjectionChangeStreamConsumerState) {
        ChangeStreamIterable<Event> watch = null;
        if (ChangeStreamConsumerState.NULL.equals(previousProjectionChangeStreamConsumerState)) {
            watch = this.events.watch(); // starts from now
        } else {
            //TODO listen only to specific events?  to operationType=insert (entityType = a) ?
            // List<Bson> pipeline = singletonList(match(in("operationType", asList("insert", "delete"))));
            BsonDocument resumeToken = resumeTokenFrom(previousProjectionChangeStreamConsumerState);
            watch = this.events.watch().resumeAfter(resumeToken); //TODO what if the resumeToken is outside of the oplog?
        }
        return watch;
    }

    private void logResumeToken(ChangeStreamDocument<Event> eventChangeStreamDocument) {
        log.info("received changestreamdocument with resumeToken: " + resumeTokenFrom(eventChangeStreamDocument));
    }

    private void saveResumeTokenAsState(ChangeStreamDocument<Event> eventChangeStreamDocument) {
        String resumeToken = resumeTokenFrom(eventChangeStreamDocument);
        ChangeStreamConsumerState changeStreamConsumerState = new ChangeStreamConsumerState(PROJECTION_ID, resumeToken);
        //write resumeToken in state
        this.changeStreamConsumerStateRepository.save(changeStreamConsumerState); // TODO what if the state is already existing?
    }

    private ChangeStreamConsumerState previousProjectionState() {
        return this.changeStreamConsumerStateRepository.findById(PROJECTION_ID).orElse(ChangeStreamConsumerState.NULL);
    }

    private String resumeTokenFrom(ChangeStreamDocument<Event> eventChangeStreamDocument) {
        BsonDocument resumeTokenBson = eventChangeStreamDocument.getResumeToken();
        return resumeTokenBson.getString("_data").asString().getValue();
    }

    private BsonDocument resumeTokenFrom(ChangeStreamConsumerState previousProjectionChangeStreamConsumerState) {
        BsonString value = new BsonString(previousProjectionChangeStreamConsumerState.getResumeToken());
        return new BsonDocument("_data", value);
    }
}
