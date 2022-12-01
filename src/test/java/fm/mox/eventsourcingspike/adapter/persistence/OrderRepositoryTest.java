package fm.mox.eventsourcingspike.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fm.mox.eventsourcingspike.domain.DomainEvent;
import fm.mox.eventsourcingspike.orders.adapter.persistence.OrderRepository;
import fm.mox.eventsourcingspike.orders.domain.Order;
import fm.mox.eventsourcingspike.orders.domain.OrderFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderRepositoryTest {

    private OrderRepository underTest;
    private Map<String, Map<Long, List<DomainEvent>>> map;

    @BeforeEach
    void setUp() {
        this.map = new HashMap<>();
        DomainEventsPersistenceAdapter inMemory = new InMemoryDomainEventsPersistenceAdapter(this.map);
        OrderFactory orderFactory = new OrderFactory();
        this.underTest = new OrderRepository(inMemory, orderFactory);
    }

    @Test
    void testSaveAndFindById() {
        Order expected = new Order("1");
        underTest.save(expected);
        log.info(this.map.toString());
        Optional<Order> actual = underTest.findById("1");
        assertEquals(actual.orElse(Order.NULL), expected);
    }

    //TODO
    @Test
    void testSaveAndFindByIdThenSavingTheSameEntityShouldFail() {
        Order expected = new Order("1");
        underTest.save(expected);
        Optional<Order> actual = underTest.findById("1");
        Order expected1 = actual.orElse(Order.NULL);
        assertEquals(expected1, expected);

        //must throw
        try {
            Order another = new Order("1");
            underTest.save(another);
            fail();
        } catch (Throwable e) {
            assertNotNull(e);
        }

        // TODO call method that alters the domain event producing new events

    }
}