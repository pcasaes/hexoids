package me.paulo.casaes.bbop.service.kafka.topics;

import me.paulo.casaes.bbop.dto.EventDto;
import me.paulo.casaes.bbop.model.DomainEvent;
import me.paulo.casaes.bbop.model.Game;
import me.paulo.casaes.bbop.model.Topics;
import me.paulo.casaes.bbop.service.ConfigurationService;
import me.paulo.casaes.bbop.service.kafka.TopicInfo;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("BoltLifeCycleTopic")
@Dependent
public class BoltLifeCycleTopic implements TopicInfo {

    private static final Logger LOGGER = Logger.getLogger(BoltLifeCycleTopic.class.getName());

    private static final int NUM_PARTITIONS = 1;

    private final ConfigurationService configurationService;

    @Inject
    public BoltLifeCycleTopic(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    private final Collection<ConsumerInfo> consumerInfos = Collections.singleton(new ConsumerInfo() {


        private final ConsumerRecord<UUID, EventDto>[] recordToOffset = new ConsumerRecord[NUM_PARTITIONS];

        @Override
        public boolean useSubscription() {
            return true;
        }

        @Override
        public Optional<Properties> consumerConfig() {
            Properties properties = new Properties();

            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "bbop-server");
            properties.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "20000");
            properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

            return Optional.of(properties);
        }

        @Override
        public void consume(DomainEvent domainEvent) {
            topic().consume(domainEvent);
        }

        @Override
        public void postConsume(Consumer<UUID, EventDto> kafkaConsumer, ConsumerRecord<UUID, EventDto> record) {
            int partition = record.partition();
            if (recordToOffset[partition] == null) {
                recordToOffset[partition] = record;
            }

            if (recordToOffset[partition].timestamp() + (configurationService.getBoltMaxDuration() + 2L) < Game.get().getClock().getTime()) {
                Map<TopicPartition, OffsetAndMetadata> commitData = Collections
                        .singletonMap(new TopicPartition(recordToOffset[partition].topic(), partition),
                                new OffsetAndMetadata(recordToOffset[partition].offset())
                        );
                kafkaConsumer.commitAsync(commitData, this::onComplete);

                recordToOffset[partition] = record;
            }
        }

        void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
            if (exception != null) {
                LOGGER.log(Level.WARNING, "Could not commit off data " + offsets, exception);
            }
        }
    });

    @Override
    public NewTopic newTopic() {
        final Map<String, String> props = new HashMap<>();
        long boltDurationFactor = this.configurationService.getBoltMaxDuration() * 2;
        props.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Math.max(60_000, boltDurationFactor)));
        props.put(TopicConfig.SEGMENT_MS_CONFIG, String.valueOf(Math.max(60_000, boltDurationFactor)));
        props.put(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.25");
        props.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);
        props.put(TopicConfig.MESSAGE_TIMESTAMP_TYPE_CONFIG, "LogAppendTime");

        return new NewTopic(topic().name(), NUM_PARTITIONS, (short) 1)
                .configs(props);
    }

    @Override
    public Topics topic() {
        return Topics.BOLT_LIFECYCLE_TOPIC;
    }

    @Override
    public Collection<ConsumerInfo> consumerInfos() {
        return this.consumerInfos;
    }
}
