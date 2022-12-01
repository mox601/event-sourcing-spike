package fm.mox.eventsourcingspike.billing.domain;

import java.util.ArrayList;
import java.util.List;

import fm.mox.eventsourcingspike.common.DomainEntity;
import fm.mox.eventsourcingspike.domain.DomainEvent;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class Bill implements DomainEntity {

    public static final Bill NULL = new Bill();

    private String id;

    private String status;

    private Long version;

    private final List<DomainEvent> uncommittedEvents;

    public Bill() {
        this.uncommittedEvents = new ArrayList<>();
        this.version = -1L;
    }

    public Bill(String id) {
        this();
        mutate(new BillCreated(id, "pending"));
    }

    public void loadFrom(List<DomainEvent> domainEvents) {
        //TODO add guard to make it only work on "empty" instances
        for (DomainEvent domainEvent : domainEvents) {
            apply(domainEvent);
        }
    }

    private void apply(DomainEvent domainEvent) {
        //TODO switch on different event types
        if (domainEvent instanceof BillCreated billCreated) {
            apply(billCreated);
        } else {
            throw new IllegalStateException("Unexpected value: " + domainEvent);
        }
    }

    private void apply(BillCreated billCreated) {
        this.id = billCreated.getEntityId();
        this.status = "pending";
        // skips appending
        //append(orderPlaced);
        nextVersion();
    }

    private void mutate(BillCreated billCreated) {
        this.id = billCreated.getEntityId();
        this.status = billCreated.getStatus();
        appendUncommitted(billCreated); //appends
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
