package fm.mox.eventsourcingspike.orders.domain;

import java.util.List;

import fm.mox.eventsourcingspike.common.DomainEntityFactory;
import fm.mox.eventsourcingspike.domain.DomainEvent;

public class OrderFactory implements DomainEntityFactory<Order, DomainEvent> {

    @Override
    public Order from(List<DomainEvent> domainEvents) {
        if (domainEvents == null || domainEvents.isEmpty()) {
            return null;
        }
        Order order = new Order();
        order.loadFrom(domainEvents);
        return order;
    }
}
