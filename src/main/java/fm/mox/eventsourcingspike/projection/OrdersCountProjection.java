package fm.mox.eventsourcingspike.projection;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoCollection;

import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsSerDe;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.Event;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.ChangeStreamConsumerStateRepository;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.OrderId;
import fm.mox.eventsourcingspike.projection.persistence.mongodb.OrderIdsRepository;
import fm.mox.eventsourcingspike.view.OrdersCountViewUpdater;
import fm.mox.eventsourcingspike.view.persistence.mongodb.OrdersCountRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrdersCountProjection implements Closeable {

    private final MongoCollection<Event> events;
    private final MongoCollection<OrderId> domainEntities;
    private final OrdersCountRepository ordersCountRepository;
    private final ChangeStreamConsumerStateRepository changeStreamConsumerStateRepository;
    private final DomainEventsSerDe domainEventsSerDe;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final OrderIdsRepository orderIdsRepository;
    private ChangeStreamDocumentConsumer changeStreamDocumentConsumer;

    public OrdersCountProjection(MongoCollection<Event> events,
                                 MongoCollection<OrderId> orderIds,
                                 OrderIdsRepository orderIdsRepository,
                                 OrdersCountRepository ordersCountRepository,
                                 ChangeStreamConsumerStateRepository changeStreamConsumerStateRepository,
                                 DomainEventsSerDe domainEventsSerDe) {
        this.events = events;
        this.ordersCountRepository = ordersCountRepository;
        this.changeStreamConsumerStateRepository = changeStreamConsumerStateRepository;
        this.domainEventsSerDe = domainEventsSerDe;
        this.domainEntities = orderIds;
        this.orderIdsRepository = orderIdsRepository;
        this.executorService = Executors.newSingleThreadExecutor();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        // 1 thread consumes change stream and updates both
        // - the temporary collection domainEntityItemRepository
        // - the current status of stream with the last resumeToken processed in stateRepository
        this.changeStreamDocumentConsumer = new ChangeStreamDocumentConsumer(this.events,
                                                                             this.domainEventsSerDe,
                                                                             this.changeStreamConsumerStateRepository,
                                                                             this.orderIdsRepository);
        this.executorService.execute(this.changeStreamDocumentConsumer);

        // another thread runs every N seconds to
        // - read from the temporary table domainEntities
        //   (using lower level mongodb apis)
        // - write the view results to another table domainEntitiesCountRepository
        OrdersCountViewUpdater ordersCountViewUpdater =
                new OrdersCountViewUpdater(this.domainEntities, this.ordersCountRepository);
        this.scheduledExecutorService.scheduleWithFixedDelay(ordersCountViewUpdater,
                                                             0L,
                                                             1L,
                                                             TimeUnit.SECONDS);
    }

    //TODO stop the consumer when needed
    @Override
    public void close() throws IOException {
        this.changeStreamDocumentConsumer.close();
        this.executorService.shutdownNow();
        this.scheduledExecutorService.shutdownNow();
    }
}
