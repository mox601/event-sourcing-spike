package fm.mox.eventsourcingspike.orders.projection.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderStatusRepository extends MongoRepository<OrderStatus, String> {

}
