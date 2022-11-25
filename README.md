# event-sourcing-spike

https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/
https://github.com/mongodb-developer/java-quick-start
https://www.mongodb.com/developer/languages/java/java-change-streams/
https://www.mongodb.com/docs/manual/changeStreams/
https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/data-formats/document-data-format-pojo/
https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/data-formats/pojo-customization/

https://www.mongodb.com/community/forums/t/resume-of-change-stream-was-not-possible-as-the-resume-point-may-no-longer-be-in-the-oplog/9303/6?u=matteo_moci

https://ashishtechmill.com/spring-boot-and-java-16-records
https://aws.amazon.com/it/blogs/database/build-a-cqrs-event-store-with-amazon-dynamodb/
https://www.youtube.com/watch?v=ROor6_NGIWU
https://hub.docker.com/_/mongo/
https://www.testcontainers.org/modules/databases/mongodb/
https://stackoverflow.com/questions/60115915/how-work-with-immutable-object-in-mongodb-and-lombook-without-bsondiscriminator
https://stackoverflow.com/a/52072594/40331

https://domaincentric.net/blog
https://domaincentric.net/blog/event-sourcing-aggregates-vs-projections
https://domaincentric.net/blog/event-sourcing-projections
https://domaincentric.net/blog/event-sourcing-projection-patterns-side-effect-handling
https://domaincentric.net/blog/event-sourcing-projections-patterns-consumer-scaling
https://domaincentric.net/blog/event-sourcing-projection-patterns-deduplication-strategies

https://medium.com/@david.truong510/jackson-polymorphic-deserialization-91426e39b96a
https://octoperf.com/blog/2018/02/01/polymorphism-with-jackson/#type-mapping

https://github.com/sigpwned/jackson-modules-java-17-sealed-classes
https://medium.com/@david.truong510/jackson-polymorphic-deserialization-91426e39b96a
https://web.archive.org/web/20200623023452/https://programmerbruce.blogspot.com/2011/05/deserialize-json-with-jackson-into.html

https://www.mongodb.com/docs/kafka-connector/current/troubleshooting/recover-from-invalid-resume-token/#std-label-kafka-troubleshoot-invalid-resume-token

https://www.mongodb.com/docs/kafka-connector/current/quick-start/

https://github.com/EventStore/EventStoreDB-Client-Java
https://developers.eventstore.com/server/v20.10/installation.html#run-with-docker
https://www.testcontainers.org/features/creating_container/





https://www.youtube.com/watch?v=rO9BXsl4AMQ
orderplaced
orderbilled
shippinglabelcreated
orderreadytoship


OrderRequested
-validated



c: SubmitOrder
e: OrderCreated
e: OrderValidated
e: PaymentProcessed
e: OrderConfirmed
e: ShippingPrepared
e: ShipmentDispatched
e: ShipmentDelivered
e: OrderCompleted

orders, shipments, payments



Mongo Change Streams
- it is exposing the oplog, so events will not be there forever, but only until the oplog reaches the time/size limits configured
- even if the events were there forever, there's no API to "get me all the events since the beginning of time"
- hard to filter by a specific domain event type
  - since it stores multiple domain events in single document: is there an API for filtering?
- a catch-up subscription is hard to implement
  - it would have to start paginating over the events on the collection
  - then, at some point switch to the real-time change stream API
  - seems complex and bug prone!

For these reasons, there's need for another component to
- reliably store all events for a long retention time
- allows for a catch-up subscription replaying all the events since the beginning of time 
  - with no hard-to-implement switch to the realtime changes

what about reacting to events within the same bounded context?
e.g. how should a process manager be implemented? what source does it listen to?
a message broker? what mb should we use? Kafka?

- one subscription (kafka consumer) per process manager, that
  - filters specific domain events
  - receives the message
  - calls the command on the aggregate
  - ack the message
    - todo what about failure modes?

- mongodb collection poller?
  - what about the pagination on a collection that is always growing?

# OrderProcessManager
This pm is responsible for reacting to domain events and sending commands
itself is a stateful entity, that changes states as it receives and sends commands
in order to do it, we handle a domain event like this: 
a Listener receives the domain event and:
1. mark the domain event as "processing"
2. loads the pm state from db
3. calls the right method on the pm (e.g. void handleEvent(DomainEvent de)). the method will:
   1. generate a command to publish
   2. generate the internal events to change its state 
4. the Listener will then: 
   1. send the command
      1. on Kafka?
   2. save the pm state
   3. mark the domain event as "consumed"

how can these things happen in a reliable way? idempotently?
there are several failure modes possible where the process could fail: 
  - mark the domain event as "processing"
    - listener will be restarted from last processed
  - send the command
    - failed to send the command. restarting the process would restart from the last "processing" domain event
  - store the pm state
    - failed to store the pm state. restarting the process would send a duplicate command. at least once command delivery
      - transactional outbox? https://microservices.io/patterns/data/transactional-outbox.html
      - message consumer must be idempotent, by tracking the IDs of the messages that it has already processed
  - mark domain event as "consumed"
    - failed to mark event as consumed. restarting the process would load the updated state and the pm should not do the work for the same event
      -  idempotent pm
