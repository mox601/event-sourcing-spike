package fm.mox.eventsourcingspike;

import fm.mox.eventsourcingspike.adapter.persistence.mongodb.Event;
import fm.mox.eventsourcingspike.adapter.persistence.mongodb.MongoEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
class EventSourcingSpikeApplicationTests {

    @Autowired
    private MongoEventRepository mongoEventRepository;

    @Test
    void contextLoadsEvent() {

        mongoEventRepository.deleteAll();

        // fetch all customers
        System.out.println("Events found with findAll():");
        System.out.println("-------------------------------");
        for (Event event : mongoEventRepository.findAll()) {
            System.out.println(event);
        }
        System.out.println();

        // save a couple of events related to 2 distinct entities
        // previous eventsCount is 0 because they are new entities, and we expect no other events on the db
        // expectedPreviousVersion = null to handle optimistic lock: we expect no other events on the db with the same id

        Event one = Event.build("a", "1", Arrays.asList("1", "2"), 0L);
        Event two = Event.build("a", "2", Arrays.asList("3", "4"), 0L);

        mongoEventRepository.save(one);
        mongoEventRepository.save(two);

        // fetch all customers
        System.out.println("Events found with findAll():");
        System.out.println("-------------------------------");
        for (Event event : mongoEventRepository.findAll()) {
            System.out.println(event);
        }
        System.out.println();

        // fetch by entity type
        System.out.println("Events found with findByEntityType:");
        System.out.println("--------------------------------");
        System.out.println(mongoEventRepository.findByEntityType("a"));

        System.out.println("Event found with findById('a-1-0'):");
        System.out.println("--------------------------------");
        System.out.println(mongoEventRepository.findById("a-1-0"));


        Event oneBis = new Event("a-1-0", 0L, "a",
                "1", null, Arrays.asList("1", "2"));
        //this fails!
        mongoEventRepository.save(oneBis);

        //expectedPreviousVersion = null doesn't work
        // org.springframework.dao.DuplicateKeyException: Write operation error on server localhost:65019. Write error: WriteError{code=11000, message='E11000 duplicate key error collection: test.event index: _id_ dup key: { _id: "1-0" }', details={}}.; nested exception is com.mongodb.MongoWriteException: Write operation error on server localhost:65019. Write error: WriteError{code=11000, message='E11000 duplicate key error collection: test.event index: _id_ dup key: { _id: "1-0" }', details={}}.

        //expectedPreviousVersion = 0 works

        //expectedPreviousVersion = 1 doesn't work:
        // org.springframework.dao.OptimisticLockingFailureException: Cannot save entity 1-0 with version 2 to collection event. Has it been modified meanwhile?

        System.out.println("Event found with findById('a-1-0'):");
        System.out.println("--------------------------------");
        System.out.println(mongoEventRepository.findById("a-1-0"));
    }

    //domain event repository


}
