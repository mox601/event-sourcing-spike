package fm.mox.eventsourcingspike.adapter.persistence;

import java.util.function.Consumer;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MongoDatabaseUtils {

    public static void printCollectionNames(MongoDatabase testDatabase1) {
        MongoIterable<String> collectionNames = testDatabase1.listCollectionNames();
        for (String string : collectionNames) {
            log.info("collection: " + string);
        }
    }

    public static <T> void printAllEvents(MongoCollection<T> mongoCollection) {
        mongoCollection.find().forEach(logItem("item: "));
    }

    private static <T> Consumer<T> logItem(String prefix) {
        return x -> log.info(prefix + x);
    }
}
