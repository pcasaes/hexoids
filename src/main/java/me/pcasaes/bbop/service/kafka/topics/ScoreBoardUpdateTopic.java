package me.pcasaes.bbop.service.kafka.topics;

import me.pcasaes.bbop.model.Topics;
import me.pcasaes.bbop.service.kafka.TopicInfo;
import me.pcasaes.bbop.service.kafka.TopicInfoPriority;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;

import javax.enterprise.context.Dependent;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Dependent
@Named("ScoreBoardUpdateTopic")
@TopicInfoPriority(TopicInfoPriority.Priority.TWO)
public class ScoreBoardUpdateTopic implements TopicInfo {

    @Override
    public NewTopic newTopic() {
        final Map<String, String> props = new HashMap<>();
        props.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(60_000));
        props.put(TopicConfig.DELETE_RETENTION_MS_CONFIG, String.valueOf(1_000));
        props.put(TopicConfig.SEGMENT_MS_CONFIG, String.valueOf(60_000));
        props.put(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.25");
        props.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT + ", " + TopicConfig.CLEANUP_POLICY_DELETE);
        props.put(TopicConfig.MESSAGE_TIMESTAMP_TYPE_CONFIG, "CreateTime");

        return new NewTopic(topic().name(), 1, (short) 1)
                .configs(props);
    }

    @Override
    public Topics topic() {
        return Topics.SCORE_BOARD_UPDATE_TOPIC;
    }


}
