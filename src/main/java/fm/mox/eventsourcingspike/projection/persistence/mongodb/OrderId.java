package fm.mox.eventsourcingspike.projection.persistence.mongodb;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Builder(builderClassName = "Builder")
@Value
@AllArgsConstructor(onConstructor_ = {@BsonCreator})
//@BsonDiscriminator not needed, apparently
public class OrderId {

    @Id
    @BsonId
    String id;

}
