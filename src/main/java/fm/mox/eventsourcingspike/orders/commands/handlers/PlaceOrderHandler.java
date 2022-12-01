package fm.mox.eventsourcingspike.orders.commands.handlers;

import fm.mox.eventsourcingspike.common.CommandHandler;
import fm.mox.eventsourcingspike.orders.adapter.persistence.OrderRepository;
import fm.mox.eventsourcingspike.orders.commands.PlaceOrder;
import fm.mox.eventsourcingspike.orders.domain.Order;

//TODO this is a general implementation that is missing the messaging infrastructure
public class PlaceOrderHandler implements CommandHandler<PlaceOrder> {

    private final OrderRepository orderRepository;

    public PlaceOrderHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void handle(PlaceOrder command) {
        //TODO reserve next id from repository?
        //create aggregate
        // TODO use a correlation id to trace all the changes related to the same command?
        Order order = new Order(command.getId());
        //save it
        this.orderRepository.save(order);
    }
}
