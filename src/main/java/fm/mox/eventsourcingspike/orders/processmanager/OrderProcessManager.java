package fm.mox.eventsourcingspike.orders.processmanager;

import java.util.ArrayList;
import java.util.List;

import fm.mox.eventsourcingspike.billing.commands.BillOrder;
import fm.mox.eventsourcingspike.common.CommandHandler;
import fm.mox.eventsourcingspike.common.DomainEntity;
import fm.mox.eventsourcingspike.domain.DomainEvent;
import fm.mox.eventsourcingspike.orders.domain.OrderPlaced;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class OrderProcessManager implements DomainEntity {

    public static final OrderProcessManager NULL = new OrderProcessManager();

    private String id;

    private Long version;

    private final List<DomainEvent> uncommittedEvents;

    public OrderProcessManager() {
        this.uncommittedEvents = new ArrayList<>();
        this.version = -1L;
    }

    public OrderProcessManager(String id) {
        this();
        mutate(new OrderProcessManagerCreated(id));
    }

    public void handle(CommandHandler<BillOrder> billOrderCommandHandler,
                       OrderPlaced orderPlaced) {
        //TODO check state to only progress if the preconditions are met
        // call the command on the handler
        billOrderCommandHandler.handle(new BillOrder(orderPlaced.getEntityId()));
        mutate(orderPlaced);
    }

    private void mutate(OrderProcessManagerCreated orderProcessManagerCreated) {
        this.id = orderProcessManagerCreated.getEntityId();
        appendUncommitted(orderProcessManagerCreated); //appends
        nextVersion();
    }

    private void mutate(OrderPlaced orderPlaced) {
        // TODO mutate state
        appendUncommitted(orderPlaced); //appends
        nextVersion();
    }

    private void appendUncommitted(DomainEvent domainEvent) {
        this.uncommittedEvents.add(domainEvent);
    }

    private void nextVersion() {
        this.version++;
    }

    public void loadFrom(List<DomainEvent> domainEvents) {
        //TODO add guard to make it only work on "empty" instances
        for (DomainEvent domainEvent : domainEvents) {
            apply(domainEvent);
        }
    }

    private void apply(DomainEvent domainEvent) {
        //TODO switch on different event types
        if (domainEvent instanceof OrderProcessManagerCreated a) {
            apply(a);
        } else {
            throw new IllegalStateException("Unexpected value: " + domainEvent);
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Long getVersion() {
        return this.version;
    }

    @Override
    public List<DomainEvent> getUncommittedEvents() {
        return this.uncommittedEvents;
    }
}
