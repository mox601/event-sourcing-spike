package fm.mox.eventsourcingspike.commands.handlers;

import fm.mox.eventsourcingspike.commands.BillOrder;

//TODO this is a general implementation that is missing the messaging infrastructure
public class BillOrderHandler implements CommandHandler<BillOrder> {

    public BillOrderHandler() {
        //bill repository
    }

    @Override
    public void handle(BillOrder billOrder) {
        //TODO create aggregate
        //Order order = new Order(command.getId());
        //todo save it
        //this.orderRepository.save(null);
    }
}
