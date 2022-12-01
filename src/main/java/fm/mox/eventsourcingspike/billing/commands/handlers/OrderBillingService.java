package fm.mox.eventsourcingspike.billing.commands.handlers;

import fm.mox.eventsourcingspike.billing.adapter.persistence.BillRepository;
import fm.mox.eventsourcingspike.billing.commands.BillOrder;
import fm.mox.eventsourcingspike.billing.domain.Bill;
import fm.mox.eventsourcingspike.common.CommandHandler;

public class OrderBillingService implements CommandHandler<BillOrder> {

    private final BillRepository billRepository;

    public OrderBillingService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    @Override
    public void handle(BillOrder billOrder) {
        Bill bill = new Bill(billOrder.getId());
        this.billRepository.save(bill);
    }

}
