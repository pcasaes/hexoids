package me.pcasaes.hexoids.service.kafka.topics;

import me.pcasaes.hexoids.model.GameTopic;
import me.pcasaes.hexoids.service.kafka.TopicInfo;
import me.pcasaes.hexoids.service.kafka.TopicInfoPriority;

import javax.enterprise.context.Dependent;
import javax.inject.Named;

@Dependent
@Named("ScoreBoardUpdateTopic")
@TopicInfoPriority(TopicInfoPriority.Priority.TWO)
public class ScoreBoardUpdateTopic implements TopicInfo {


    @Override
    public GameTopic topic() {
        return GameTopic.SCORE_BOARD_UPDATE_TOPIC;
    }


}
