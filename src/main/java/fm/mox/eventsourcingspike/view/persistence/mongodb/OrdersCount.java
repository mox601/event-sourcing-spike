package fm.mox.eventsourcingspike.view.persistence.mongodb;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Builder(builderClassName = "Builder")
@Value
@AllArgsConstructor(onConstructor_ = {@BsonCreator})
//@BsonDiscriminator not needed, apparently
public class OrdersCount {

    public static final OrdersCount NULL = new OrdersCount("-1", -1L);

    @Id
    @BsonId
    String id;

    @BsonProperty("count")
    Long count;

    // TODO make it more interesting, e.g. with createdCount and canceledCount
}
