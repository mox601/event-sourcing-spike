package fm.mox.eventsourcingspike.domain.processmanager;

import java.util.List;

import fm.mox.eventsourcingspike.common.DomainEntityFactory;
import fm.mox.eventsourcingspike.domain.DomainEvent;

public class OrderProcessManagerFactory implements DomainEntityFactory<OrderProcessManager, DomainEvent> {

    @Override
    public OrderProcessManager from(List<DomainEvent> domainEvents) {
        if (domainEvents == null || domainEvents.isEmpty()) {
            return null;
        }
        OrderProcessManager orderProcessManager = new OrderProcessManager();
        orderProcessManager.loadFrom(domainEvents);
        return orderProcessManager;
    }
}
