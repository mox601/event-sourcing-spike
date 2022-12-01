package fm.mox.eventsourcingspike.orders.projection.persistence.mongodb;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * State. Represents the state of the event changes stream that was processed
 */
@Builder(builderClassName = "Builder")
@Value
@AllArgsConstructor(onConstructor_ = {@BsonCreator})
//@BsonDiscriminator not needed, apparently
public class ChangeStreamConsumerState {

    public static final ChangeStreamConsumerState NULL = new ChangeStreamConsumerState("-1", "-1");
    @Id
    @BsonId
    String id; //projection/consumer id

    //last processed resumeToken, use it to restart with resumeAfter(resumeToken)
    @BsonProperty("resumeToken")
    String resumeToken;
}
