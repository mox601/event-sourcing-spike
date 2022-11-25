package fm.mox.eventsourcingspike.commands.handlers;

import fm.mox.eventsourcingspike.adapter.persistence.OrderRepository;
import fm.mox.eventsourcingspike.commands.PlaceOrder;
import fm.mox.eventsourcingspike.domain.Order;

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
        Order order = new Order(command.getId());
        //save it
        this.orderRepository.save(order);
    }
}
