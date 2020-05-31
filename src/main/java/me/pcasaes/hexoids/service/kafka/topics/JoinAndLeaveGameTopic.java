package me.pcasaes.hexoids.service.kafka.topics;

import me.pcasaes.hexoids.model.GameTopic;
import me.pcasaes.hexoids.service.kafka.TopicInfo;
import me.pcasaes.hexoids.service.kafka.TopicInfoPriority;

import javax.enterprise.context.Dependent;
import javax.inject.Named;

@Named("JoinGameTopic")
@Dependent
@TopicInfoPriority(TopicInfoPriority.Priority.ONE)
public class JoinAndLeaveGameTopic implements TopicInfo {


    @Override
    public GameTopic topic() {
        return GameTopic.JOIN_GAME_TOPIC;
    }

}
