package fm.mox.eventsourcingspike.orders.view;

import com.mongodb.client.MongoCollection;

import fm.mox.eventsourcingspike.orders.projection.persistence.mongodb.OrderStatus;
import fm.mox.eventsourcingspike.orders.view.persistence.mongodb.OrdersCount;
import fm.mox.eventsourcingspike.orders.view.persistence.mongodb.OrdersCountRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrdersCountViewUpdater implements Runnable {

    public static final String VIEW_ID = "orders-count";

    private final MongoCollection<OrderStatus> orderIds;

    // one repository per view
    private final OrdersCountRepository ordersCountRepository;

    public OrdersCountViewUpdater(MongoCollection<OrderStatus> orderIds,
                                  OrdersCountRepository ordersCountRepository) {
        this.orderIds = orderIds;
        this.ordersCountRepository = ordersCountRepository;
    }

    @Override
    public void run() {
        //TODO count the distinct entityIds from where ChangeStreamDocumentConsumer writes them
        // domainEntityItemMongoCollection.countDocuments()
        // use lower level MongoCollection to push predicates to the db
        long pendingOrdersCount = this.orderIds.countDocuments();
        log.info("pending orders count: " + pendingOrdersCount);
        // TODO count also confirmed orders
        long confirmedCount = 0L;
        //write them in a specific collection that implements the view
        OrdersCount entity = new OrdersCount(VIEW_ID, pendingOrdersCount, confirmedCount);
        //use a repository to save the entity
        this.ordersCountRepository.save(entity);
    }
}
