package fm.mox.eventsourcingspike.orders.adapter.persistence;

import java.util.List;
import java.util.Optional;

import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsPersistenceAdapter;
import fm.mox.eventsourcingspike.common.DomainEntityFactory;
import fm.mox.eventsourcingspike.domain.DomainEvent;
import fm.mox.eventsourcingspike.orders.domain.Order;
import fm.mox.eventsourcingspike.orders.domain.OrderFactory;

public class OrderRepository {

    private static final String ORDER_TYPE = "order";

    private final DomainEventsPersistenceAdapter domainEventsPersistenceAdapter;
    private final DomainEntityFactory<Order, DomainEvent> orderFactory;

    public OrderRepository(DomainEventsPersistenceAdapter domainEventsPersistenceAdapter,
                           DomainEntityFactory<Order, DomainEvent> orderFactory) {
        this.domainEventsPersistenceAdapter = domainEventsPersistenceAdapter;
        this.orderFactory = orderFactory;
    }

    public void save(Order order) {
        String id = order.getId();
        List<DomainEvent> events = order.getUncommittedEvents();
        Long version = order.getVersion();
        this.domainEventsPersistenceAdapter.save(ORDER_TYPE, id, events, version);
    }

    public Optional<Order> findById(String entityId) {
        List<DomainEvent> byId = this.domainEventsPersistenceAdapter.findById(ORDER_TYPE, entityId);
        Order order = this.orderFactory.from(byId);
        return Optional.ofNullable(order);
    }
}
