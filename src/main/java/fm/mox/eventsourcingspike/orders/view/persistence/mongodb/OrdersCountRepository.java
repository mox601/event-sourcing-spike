package fm.mox.eventsourcingspike.orders.view.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrdersCountRepository extends MongoRepository<OrdersCount, String> {

}
