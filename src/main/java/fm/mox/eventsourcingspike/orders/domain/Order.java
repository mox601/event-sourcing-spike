package fm.mox.eventsourcingspike.orders.domain;

import java.util.ArrayList;
import java.util.List;

import fm.mox.eventsourcingspike.common.DomainEntity;
import fm.mox.eventsourcingspike.domain.DomainEvent;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class Order implements DomainEntity {

    public static final Order NULL = new Order();
    private String id;

    private String status;

    private Long version;

    private final List<DomainEvent> uncommittedEvents;

    public Order() {
        this.uncommittedEvents = new ArrayList<>();
        this.version = -1L;
    }

    public Order(String id) {
        this();
        mutate(new OrderPlaced(id, "pending"));
    }

    // business method -> check invariants -> call mutate
    // events -> call apply

    //  methods with business logic check invariants and call addEvent
    // addEvent has a switch that calls the corresponding apply and then adds the event to the list
    // apply methods receive the method and alter the fields

    public void loadFrom(List<DomainEvent> domainEvents) {
        //TODO add guard to make it only work on "empty" instances
        for (DomainEvent domainEvent : domainEvents) {
            apply(domainEvent);
        }
    }

    private void apply(DomainEvent domainEvent) {
        //TODO switch on different event types
        if (domainEvent instanceof OrderPlaced a) {
            apply(a);
        } else {
            throw new IllegalStateException("Unexpected value: " + domainEvent);
        }
    }

    private void apply(OrderPlaced orderPlaced) {
        this.id = orderPlaced.getEntityId();
        this.status = "pending";
        // skips appending
        //append(orderPlaced);
        nextVersion();
    }

    private void mutate(OrderPlaced orderPlaced) {
        this.id = orderPlaced.getEntityId();
        this.status = orderPlaced.getStatus();
        appendUncommitted(orderPlaced); //appends
        nextVersion();
    }

    private void appendUncommitted(DomainEvent domainEvent) {
        this.uncommittedEvents.add(domainEvent);
    }

    private void nextVersion() {
        this.version++;
    }

    @Override
    public String getId() {
        return id;
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
