package fm.mox.eventsourcingspike.billing.domain;

import java.util.List;

import fm.mox.eventsourcingspike.common.DomainEntityFactory;
import fm.mox.eventsourcingspike.domain.DomainEvent;

public class BillFactory implements DomainEntityFactory<Bill, DomainEvent> {

    @Override
    public Bill from(List<DomainEvent> domainEvents) {
        if (domainEvents == null || domainEvents.isEmpty()) {
            return null;
        }
        Bill bill = new Bill();
        bill.loadFrom(domainEvents);
        return bill;
    }
}
