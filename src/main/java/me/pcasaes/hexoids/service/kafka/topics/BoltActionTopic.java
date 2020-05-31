package me.pcasaes.hexoids.service.kafka.topics;

import me.pcasaes.hexoids.model.GameTopic;
import me.pcasaes.hexoids.service.kafka.TopicInfo;
import me.pcasaes.hexoids.service.kafka.TopicInfoPriority;

import javax.enterprise.context.Dependent;
import javax.inject.Named;

@Named("BoltActionTopic")
@Dependent
@TopicInfoPriority(TopicInfoPriority.Priority.TWO)
public class BoltActionTopic implements TopicInfo {

    @Override
    public GameTopic topic() {
        return GameTopic.BOLT_ACTION_TOPIC;
    }


}
