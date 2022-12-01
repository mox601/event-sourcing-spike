package fm.mox.eventsourcingspike.orders.processmanager;

import java.util.List;
import java.util.Optional;

import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsPersistenceAdapter;
import fm.mox.eventsourcingspike.domain.DomainEvent;

public class OrderProcessManagerRepository {

    private static final String ENTITY_TYPE = "order-process-manager";

    private final DomainEventsPersistenceAdapter domainEventsPersistenceAdapter;
    private final OrderProcessManagerFactory orderProcessManagerFactory;

    public OrderProcessManagerRepository(DomainEventsPersistenceAdapter domainEventsPersistenceAdapter,
                                         OrderProcessManagerFactory orderProcessManagerFactory) {
        this.domainEventsPersistenceAdapter = domainEventsPersistenceAdapter;
        this.orderProcessManagerFactory = orderProcessManagerFactory;
    }

    public void save(OrderProcessManager orderProcessManager) {
        String id = orderProcessManager.getId();
        List<DomainEvent> events = orderProcessManager.getUncommittedEvents();
        Long version = orderProcessManager.getVersion();
        this.domainEventsPersistenceAdapter.save(ENTITY_TYPE, id, events, version);
    }

    public Optional<OrderProcessManager> findById(String entityId) {
        List<DomainEvent> byId = this.domainEventsPersistenceAdapter.findById(ENTITY_TYPE, entityId);
        OrderProcessManager orderProcessManager = this.orderProcessManagerFactory.from(byId);
        return Optional.ofNullable(orderProcessManager);
    }
}
