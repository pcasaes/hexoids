package me.pcasaes.bbop.service.kafka.topics;

import me.pcasaes.bbop.model.DomainEvent;
import me.pcasaes.bbop.model.Game;
import me.pcasaes.bbop.model.Topics;
import me.pcasaes.bbop.service.kafka.TopicInfo;
import me.pcasaes.bbop.service.kafka.TopicInfoPriority;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.TopicConfig;

import javax.enterprise.context.Dependent;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@Named("JoinGameTopic")
@Dependent
@TopicInfoPriority(TopicInfoPriority.Priority.ONE)
public class JoinAndLeaveGameTopic implements TopicInfo {

    @Override
    public NewTopic newTopic() {
        final Map<String, String> props = new HashMap<>();
        props.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(24 * 60 * 60_000L)); //for now will expire someone after 24 hours
        props.put(TopicConfig.DELETE_RETENTION_MS_CONFIG, String.valueOf(1_000));
        props.put(TopicConfig.SEGMENT_MS_CONFIG, String.valueOf(60_000));
        props.put(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.25");
        props.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT + ", " + TopicConfig.CLEANUP_POLICY_DELETE);
        props.put(TopicConfig.MESSAGE_TIMESTAMP_TYPE_CONFIG, "LogAppendTime");

        return new NewTopic(topic().name(), 1, (short) 1)
                .configs(props);
    }

    @Override
    public Topics topic() {
        return Topics.JOIN_GAME_TOPIC;
    }

    @Override
    public Collection<ConsumerInfo> consumerInfos() {
        List<ConsumerInfo> consumers = new ArrayList<>();
        consumers.add(topic()::consume);

        consumers.add(new ConsumerInfo() {
            @Override
            public void consume(DomainEvent domainEvent) {
                Game.get().getPlayers().consumeFromJoinAndLeaveForServerUpdates(domainEvent);
            }

            @Override
            public boolean useSubscription() {
                return true;
            }

            @Override
            public Optional<Properties> consumerConfig() {
                Properties properties = new Properties();

                properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "bbop-server");
                properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

                return Optional.of(properties);
            }
        });

        return consumers;
    }
}
