package me.pcasaes.hexoids.service.kafka.topics;

import me.pcasaes.hexoids.model.DomainEvent;
import me.pcasaes.hexoids.model.GameTopic;
import me.pcasaes.hexoids.service.kafka.TopicInfo;
import me.pcasaes.hexoids.service.kafka.TopicInfoPriority;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.TopicConfig;

import javax.enterprise.context.Dependent;
import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@Dependent
@Named("ScoreBoardControlTopic")
@TopicInfoPriority(TopicInfoPriority.Priority.TWO)
public class ScoreBoardControlTopic implements TopicInfo {

    private final Collection<ConsumerInfo> consumerInfos = Collections.singleton(new ConsumerInfo() {

        @Override
        public Optional<Properties> consumerConfig() {
            Properties properties = new Properties();

            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "hexoids-server");
            properties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10000");
            properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

            return Optional.of(properties);
        }

        @Override
        public void consume(DomainEvent domainEvent) {
            topic().consume(domainEvent);
        }
    });

    @Override
    public NewTopic newTopic() {
        final Map<String, String> props = new HashMap<>();
        props.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(60_000L));
        props.put(TopicConfig.DELETE_RETENTION_MS_CONFIG, String.valueOf(60_000L));
        props.put(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG, String.valueOf(60_000L));
        props.put(TopicConfig.SEGMENT_MS_CONFIG, String.valueOf(60_000));
        props.put(TopicConfig.SEGMENT_BYTES_CONFIG, String.valueOf(1024 * 1024 * 128));
        props.put(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.25");
        props.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT + ", " + TopicConfig.CLEANUP_POLICY_DELETE);
        props.put(TopicConfig.MESSAGE_TIMESTAMP_TYPE_CONFIG, "LogAppendTime");

        return new NewTopic(topic().name(), 1, (short) 1)
                .configs(props);
    }

    @Override
    public GameTopic topic() {
        return GameTopic.SCORE_BOARD_CONTROL_TOPIC;
    }

    @Override
    public Collection<ConsumerInfo> consumerInfos() {
        return this.consumerInfos;
    }
}
