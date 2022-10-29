package fm.mox.eventsourcingspike.commands.handlers;

import fm.mox.eventsourcingspike.adapter.persistence.OrderRepository;
import fm.mox.eventsourcingspike.commands.PlaceOrder;
import fm.mox.eventsourcingspike.domain.Order;

public class PlaceOrderHandler implements CommandHandler<PlaceOrder> {

    private OrderRepository orderRepository;

    @Override
    public void handle(PlaceOrder command) {
        //TODO reserve next id from repository
        //create aggregate
        Order order = new Order("some id from next generator");
        //save it
        this.orderRepository.save(order);
    }
}
