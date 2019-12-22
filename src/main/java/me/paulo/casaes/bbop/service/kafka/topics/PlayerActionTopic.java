package me.paulo.casaes.bbop.service.kafka.topics;

import me.paulo.casaes.bbop.model.Topics;
import me.paulo.casaes.bbop.service.kafka.BTopic;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;

import javax.enterprise.context.Dependent;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Named("PlayerActionTopic")
@Dependent
public class PlayerActionTopic implements BTopic {

    @Override
    public NewTopic get() {
        final Map<String, String> props = new HashMap<>();
        props.put(TopicConfig.DELETE_RETENTION_MS_CONFIG, String.valueOf(1_000));
        props.put(TopicConfig.SEGMENT_MS_CONFIG, String.valueOf(1_000));
        props.put(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.1");
        props.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);
        props.put(TopicConfig.MESSAGE_TIMESTAMP_TYPE_CONFIG, "CreateTime");

        return new NewTopic(Topics.PlayerActionTopic.name(), 1, (short) 1)
                .configs(props);
    }
}
