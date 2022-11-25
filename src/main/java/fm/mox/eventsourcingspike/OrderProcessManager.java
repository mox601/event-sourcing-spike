package fm.mox.eventsourcingspike;

import java.util.ArrayList;
import java.util.List;

import fm.mox.eventsourcingspike.common.DomainEntity;
import fm.mox.eventsourcingspike.domain.DomainEvent;
import fm.mox.eventsourcingspike.domain.OrderPlaced;
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

    public void handle(OrderPlaced orderPlaced) {
        // call the command

        // mutate
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
