package fm.mox.eventsourcingspike.orders.projection;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoCollection;

import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsSerDe;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.Event;
import fm.mox.eventsourcingspike.orders.projection.persistence.mongodb.ChangeStreamConsumerStateRepository;
import fm.mox.eventsourcingspike.orders.projection.persistence.mongodb.OrderStatus;
import fm.mox.eventsourcingspike.orders.projection.persistence.mongodb.OrderStatusRepository;
import fm.mox.eventsourcingspike.orders.view.OrdersCountViewUpdater;
import fm.mox.eventsourcingspike.orders.view.persistence.mongodb.OrdersCountRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Starts 2 processes that make the orders count view possible
 */
@Slf4j
public class OrdersCountProjection implements Closeable {

    private final MongoCollection<Event> events;
    private final MongoCollection<OrderStatus> domainEntities;
    private final OrdersCountRepository ordersCountRepository;
    private final ChangeStreamConsumerStateRepository changeStreamConsumerStateRepository;
    private final DomainEventsSerDe domainEventsSerDe;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final OrderStatusRepository orderStatusRepository;
    private OrderStatusProjectionUpdater orderStatusProjectionUpdater;

    public OrdersCountProjection(MongoCollection<Event> events,
                                 MongoCollection<OrderStatus> orderIds,
                                 OrderStatusRepository orderStatusRepository,
                                 OrdersCountRepository ordersCountRepository,
                                 ChangeStreamConsumerStateRepository changeStreamConsumerStateRepository,
                                 DomainEventsSerDe domainEventsSerDe) {
        this.events = events;
        this.ordersCountRepository = ordersCountRepository;
        this.changeStreamConsumerStateRepository = changeStreamConsumerStateRepository;
        this.domainEventsSerDe = domainEventsSerDe;
        this.domainEntities = orderIds;
        this.orderStatusRepository = orderStatusRepository;
        this.executorService = Executors.newSingleThreadExecutor();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        // 1 thread consumes change stream and updates both
        // - the temporary collection domainEntityItemRepository
        // - the current status of stream with the last resumeToken processed in stateRepository
        this.orderStatusProjectionUpdater = new OrderStatusProjectionUpdater(this.events,
                                                                             this.domainEventsSerDe,
                                                                             this.changeStreamConsumerStateRepository,
                                                                             this.orderStatusRepository);
        this.executorService.execute(this.orderStatusProjectionUpdater);

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
        this.orderStatusProjectionUpdater.close();
        this.executorService.shutdownNow();
        this.scheduledExecutorService.shutdownNow();
    }
}
