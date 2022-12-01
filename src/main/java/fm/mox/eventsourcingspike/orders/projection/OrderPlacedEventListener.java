package fm.mox.eventsourcingspike.orders.projection;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bson.BsonDocument;
import org.bson.BsonString;

import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsSerDe;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.Event;
import fm.mox.eventsourcingspike.billing.commands.BillOrder;
import fm.mox.eventsourcingspike.common.CommandHandler;
import fm.mox.eventsourcingspike.domain.DomainEvent;
import fm.mox.eventsourcingspike.orders.domain.OrderPlaced;
import fm.mox.eventsourcingspike.orders.processmanager.OrderProcessManager;
import fm.mox.eventsourcingspike.orders.processmanager.OrderProcessManagerRepository;
import fm.mox.eventsourcingspike.orders.projection.persistence.mongodb.ChangeStreamConsumerState;
import fm.mox.eventsourcingspike.orders.projection.persistence.mongodb.ChangeStreamConsumerStateRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes the mongod change stream and writes both - to a state - to an intermediate collection that prepares the data for the actual
 * view
 */
@Slf4j
public class OrderPlacedEventListener implements Runnable, Closeable {

    public static final String ORDER_PLACED_EVENT_LISTENER_ID = "order-placed-event-listener";
    volatile boolean shouldRun;
    private final MongoCollection<Event> events;
    private final DomainEventsSerDe domainEventsSerDe;
    private final ChangeStreamConsumerStateRepository changeStreamConsumerStateRepository;

    private final OrderProcessManagerRepository orderProcessManagerRepository;
    private final CommandHandler<BillOrder> billOrderCommandHandler;

    public OrderPlacedEventListener(MongoCollection<Event> events,
                                    DomainEventsSerDe domainEventsSerDe,
                                    ChangeStreamConsumerStateRepository changeStreamConsumerStateRepository,
                                    OrderProcessManagerRepository orderProcessManagerRepository,
                                    CommandHandler<BillOrder> billOrderHandler) {
        this.events = events;
        this.domainEventsSerDe = domainEventsSerDe;
        this.changeStreamConsumerStateRepository = changeStreamConsumerStateRepository;
        this.orderProcessManagerRepository = orderProcessManagerRepository;
        this.billOrderCommandHandler = billOrderHandler;
        this.shouldRun = true;
    }

    @Override
    public void run() {
        //TODO retry if exception
        ChangeStreamConsumerState previousProjectionChangeStreamConsumerState = previousProjectionStateOrNull();
        log.info("initial consumerstate: " + previousProjectionChangeStreamConsumerState);
        // TODO also deal with the possibility that watch() throws exception, wrapping in a try-catch
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
                    Optional<OrderPlaced> orderPlacedOpt =
                            findFirstDomainEventAndDeserialize(fullDocument,
                                                               e -> e instanceof OrderPlaced,
                                                               e -> (OrderPlaced) e);
                    orderPlacedOpt.ifPresent(this::process);
                }
                saveResumeTokenAsListenerState(eventChangeStreamDocument);
            }
            log.info("stopped");
        }
        log.info("watch iterator closed");
    }

    private void process(OrderPlaced orderPlaced) {
        log.info("processing domain event: " + orderPlaced);
        // TODO init process manager for handling payments and shipping
                        /*
                        load process manager by order id
                            it's event sourced itself
                        make it handle the orderplaced
                            it will call a command on another aggregate

                        store the process manager events
                         */
        String entityId = orderPlaced.getEntityId();
        //TODO how to make it idempotent? we are doing 2 things across datastores
        // the command handler changes another aggregate state
        // this PM is saved to its repository
        OrderProcessManager byId = this.orderProcessManagerRepository.findById(entityId)
                .orElse(new OrderProcessManager(entityId));
        byId.handle(this.billOrderCommandHandler, orderPlaced);
        this.orderProcessManagerRepository.save(byId);
        // TODO process "idempotently" each message, e.g. writing to a collection to build the view
        // TODO what happens if it was already there?
    }

    private <T> Optional<T> findFirstDomainEventAndDeserialize(Event fullDocument, Predicate<DomainEvent> domainEventPredicate,
                                                               Function<DomainEvent, T> domainEventOrderPlacedFunction) {
        return fullDocument
                .getSerializedDomainEvents()
                .stream()
                .map(this.domainEventsSerDe::deserialize)
                .filter(domainEventPredicate)
                .map(domainEventOrderPlacedFunction)
                .findFirst();
    }

    // TODO call it when the app is stopped
    @Override
    public void close() throws IOException {
        log.info("stopping");
        this.shouldRun = false;
    }

    /**
     * TODO decouple from mongodb technology - use an event bus? Kafka?
     * watch Or Resume After Token
     * this listener can be stopped and re-started, it will resume from where it stopped
     *         // or if it's the very first time it runs, will start from latest
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

    private void saveResumeTokenAsListenerState(ChangeStreamDocument<Event> eventChangeStreamDocument) {
        String resumeToken = resumeTokenFrom(eventChangeStreamDocument);
        ChangeStreamConsumerState changeStreamListenerState = new ChangeStreamConsumerState(ORDER_PLACED_EVENT_LISTENER_ID, resumeToken);
        //write resumeToken in state
        // TODO what if the state is already existing? we only have 1 thread writing so it should be ok
        this.changeStreamConsumerStateRepository.save(changeStreamListenerState);
    }

    private ChangeStreamConsumerState previousProjectionStateOrNull() {
        return this.changeStreamConsumerStateRepository.findById(ORDER_PLACED_EVENT_LISTENER_ID)
                .orElse(ChangeStreamConsumerState.NULL);
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
