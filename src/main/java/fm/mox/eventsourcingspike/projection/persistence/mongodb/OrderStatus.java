package fm.mox.eventsourcingspike.projection.persistence.mongodb;

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
public class OrderStatus {

    @Id
    @BsonId
    String id;

    @BsonProperty
    String status;

}
