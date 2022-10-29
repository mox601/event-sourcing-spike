package fm.mox.eventsourcingspike.projection;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.BsonDocument;
import org.bson.BsonString;

import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsSerDe;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.Event;
import fm.mox.eventsourcingspike.domain.OrderPlaced;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.OrderId;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.OrderIdsRepository;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.ChangeStreamConsumerState;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.ChangeStreamConsumerStateRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes the mongod change stream and writes both - to a state - to an intermediate collection that prepares the data for the actual
 * view
 */
@Slf4j
public class ChangeStreamDocumentConsumer implements Runnable, Closeable {

    public static final String PROJECTION_ID = "fixed";
    volatile boolean shouldRun;
    private final MongoCollection<Event> events;
    private final DomainEventsSerDe domainEventsSerDe;
    private final ChangeStreamConsumerStateRepository changeStreamConsumerStateRepository;
    private final OrderIdsRepository orderIdsRepository;

    public ChangeStreamDocumentConsumer(MongoCollection<Event> events,
                                        DomainEventsSerDe domainEventsSerDe,
                                        ChangeStreamConsumerStateRepository changeStreamConsumerStateRepository,
                                        OrderIdsRepository orderIdsRepository) {
        this.events = events;
        this.domainEventsSerDe = domainEventsSerDe;
        this.changeStreamConsumerStateRepository = changeStreamConsumerStateRepository;
        this.orderIdsRepository = orderIdsRepository;
        this.shouldRun = true;
    }

    @Override
    public void run() {
        //TODO retry if exception
        ChangeStreamConsumerState previousProjectionChangeStreamConsumerState = previousProjectionState();
        log.info("initial projectionState: " + previousProjectionChangeStreamConsumerState);
        // TODO also deal with the possibility that watch() throws exception,
        //  wrapping in a try-catch and restarting it
        ChangeStreamIterable<Event> watch = watchResumingFrom(previousProjectionChangeStreamConsumerState);

        // doesn't use this.watch.forEach(...); uses iterator so we can better control when to stop
        try (MongoCursor<ChangeStreamDocument<Event>> iterator = watch.iterator()) {
            //TODO deal with the thread being stopped
            while (this.shouldRun && iterator.hasNext()) {
                ChangeStreamDocument<Event> eventChangeStreamDocument = iterator.next();
                logResumeToken(eventChangeStreamDocument);
                String resumeToken = resumeTokenFrom(eventChangeStreamDocument);
                log.info("csd: " + eventChangeStreamDocument);
                //extract
                Event fullDocument = eventChangeStreamDocument.getFullDocument();
                //deserialize
                //TODO catch exceptions for deserialization and when writing state
                if (fullDocument != null) {
                    List<OrderPlaced> orderPlacedEvents = fullDocument
                            .getSerializedDomainEvents()
                            .stream()
                            .map(this.domainEventsSerDe::deserialize)
                            .filter(e -> e instanceof OrderPlaced)
                            .map(e -> (OrderPlaced) e)
                            .toList();

                    log.info("TODO process these orderPlacedEvents: " + orderPlacedEvents);

                    List<OrderId> orderIds = new ArrayList<>();
                    for (OrderPlaced orderPlaced : orderPlacedEvents) {
                        log.info("processing orderPlaced: " + orderPlaced);
                        orderIds.add(new OrderId(orderPlaced.getEntityId()));
                    }
                    // we assume only 1 thread is consuming the change stream to update this projection
                    // TODO process "idempotently" each message, e.g. writing to a collection to build the view
                    // TODO what happens if it was already there?
                    this.orderIdsRepository.saveAll(orderIds);
                }
                saveResumeTokenAsState(eventChangeStreamDocument);
            }
            log.info("stopped");
        }
        log.info("watch iterator closed");
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
