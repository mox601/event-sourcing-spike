package fm.mox.eventsourcingspike.billing.adapter.persistence;

import java.util.List;
import java.util.Optional;

import fm.mox.eventsourcingspike.adapter.persistence.DomainEventsPersistenceAdapter;
import fm.mox.eventsourcingspike.billing.domain.Bill;
import fm.mox.eventsourcingspike.billing.domain.BillFactory;
import fm.mox.eventsourcingspike.common.DomainEntityFactory;
import fm.mox.eventsourcingspike.domain.DomainEvent;

public class BillRepository {

    private static final String BILL_TYPE = "bill";

    private final DomainEventsPersistenceAdapter domainEventsPersistenceAdapter;
    private final DomainEntityFactory<Bill, DomainEvent> billFactory;

    public BillRepository(DomainEventsPersistenceAdapter domainEventsPersistenceAdapter,
                          DomainEntityFactory<Bill, DomainEvent> billFactory) {
        this.domainEventsPersistenceAdapter = domainEventsPersistenceAdapter;
        this.billFactory = billFactory;
    }

    public void save(Bill bill) {
        String id = bill.getId();
        List<DomainEvent> events = bill.getUncommittedEvents();
        Long version = bill.getVersion();
        this.domainEventsPersistenceAdapter.save(BILL_TYPE, id, events, version);
    }

    public Optional<Bill> findById(String entityId) {
        List<DomainEvent> byId = this.domainEventsPersistenceAdapter.findById(BILL_TYPE, entityId);
        Bill bill = this.billFactory.from(byId);
        return Optional.ofNullable(bill);
    }
}
