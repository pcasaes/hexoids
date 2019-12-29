package me.paulo.casaes.bbop.service.kafka;

import me.paulo.casaes.bbop.model.Topics;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Optional;
import java.util.Properties;

public interface TopicInfo {

    NewTopic newTopic();

    Topics topic();

    boolean useSubscription();

    Optional<Properties> consumerConfig();
}
