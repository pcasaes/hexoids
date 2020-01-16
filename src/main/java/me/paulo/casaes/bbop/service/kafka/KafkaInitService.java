package me.paulo.casaes.bbop.service.kafka;

import io.quarkus.runtime.StartupEvent;
import me.paulo.casaes.bbop.model.Topics;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.TopicExistsException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class KafkaInitService {


    private static final Logger LOGGER = Logger.getLogger(KafkaInitService.class.getName());

    private KafkaService kafkaService;

    private Instance<TopicInfo> topicsFactory;

    private Instance<KafkaConsumerService> kafkaConsumerServiceFactory;
    private Map<Topics, KafkaConsumerService> kafkaConsumerServiceMap = new ConcurrentHashMap<>();

    private KafkaAdmin kafkaAdmin;

    KafkaInitService() {
        //required for CDI normal scoped beans
    }

    @Inject
    public KafkaInitService(KafkaService kafkaService,
                            @Any Instance<TopicInfo> topicsFactory,
                            KafkaAdmin kafkaAdmin,
                            Instance<KafkaConsumerService> kafkaConsumerServiceFactory) {
        this.kafkaService = kafkaService;
        this.topicsFactory = topicsFactory;
        this.kafkaAdmin = kafkaAdmin;
        this.kafkaConsumerServiceFactory = kafkaConsumerServiceFactory;
    }

    public void startup(@Observes StartupEvent event) {
        // eager load
    }

    @PostConstruct
    void start() {
        LOGGER.info("Starting up kafka consumers");
        try {
            this.kafkaAdmin.execute(this::setup);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Error while initializing topics", ex);
        }

        for (TopicInfoPriority.Priority priority : TopicInfoPriority.Priority.values()) {
            List<BooleanSupplier> starting = this.topicsFactory
                    .select(TopicInfoPriority.Literal.of(priority))
                    .stream()
                    .map(topic -> {
                        KafkaConsumerService service = kafkaConsumerServiceFactory.get();
                        kafkaConsumerServiceMap.put(topic.topic(), service);
                        service.start(topic);

                        this.topicsFactory.destroy(topic);

                        return (BooleanSupplier) service::isStarted;
                    })
                    .collect(Collectors.toList());

            this.finishStarting(starting);
        }
        LOGGER.info("Finished starting up kafka consumers");
        this.kafkaService.setOkToConnect(true);

    }

    private void finishStarting(List<BooleanSupplier> starting) {
        starting
                .forEach(s -> {
                    for (int i = 0; !s.getAsBoolean() && i < 100; i++) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
    }

    @PreDestroy
    public void stop() {
        kafkaConsumerServiceMap
                .values()
                .forEach(kafkaConsumerServiceFactory::destroy);
    }


    public void init() {
        LOGGER.info("Initializing topics");
    }

    private void setup(AdminClient adminClient) {
        List<NewTopic> newTopics = StreamSupport
                .stream(topicsFactory.spliterator(),
                        false)
                .map(this::getNewTopics)
                .collect(Collectors.toList());

        adminClient
                .createTopics(newTopics)
                .values()
                .entrySet()
                .forEach(this::handelNewTopicCreation);

    }

    private NewTopic getNewTopics(TopicInfo topic) {
        try {
            return topic.newTopic();
        } finally {
            this.topicsFactory.destroy(topic);
        }
    }

    private void handelNewTopicCreation(Map.Entry<String, KafkaFuture<Void>> entry) {
        final KafkaFuture<Void> future = entry.getValue();
        final String topic = entry.getKey();

        try {
            future.get(10, TimeUnit.SECONDS);
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("Topic " + topic + " created");
            }

        } catch (InterruptedException ex) {
            LOGGER.warning(ex.getMessage());
            Thread.currentThread().interrupt();
        } catch (TimeoutException ex) {
            LOGGER.log(Level.SEVERE, "Could not create topic " + topic, ex);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof TopicExistsException) {
                LOGGER.fine("Topic " + topic + " already exists: " + ex.getMessage());
            } else {
                LOGGER.log(Level.SEVERE, "Could not create topic " + topic, ex);
            }
        }
    }

}
