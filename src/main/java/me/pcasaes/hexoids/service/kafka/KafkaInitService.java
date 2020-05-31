package me.pcasaes.hexoids.service.kafka;

import io.quarkus.runtime.StartupEvent;
import me.pcasaes.hexoids.model.GameTopic;

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
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class KafkaInitService {


    private static final Logger LOGGER = Logger.getLogger(KafkaInitService.class.getName());

    private KafkaService kafkaService;

    private Instance<TopicInfo> topicsFactory;

    private Instance<KafkaConsumerService> kafkaConsumerServiceFactory;
    private Map<GameTopic, KafkaConsumerService> kafkaConsumerServiceMap = new ConcurrentHashMap<>();

    KafkaInitService() {
        //required for CDI normal scoped beans
    }

    @Inject
    public KafkaInitService(KafkaService kafkaService,
                            @Any Instance<TopicInfo> topicsFactory,
                            Instance<KafkaConsumerService> kafkaConsumerServiceFactory) {
        this.kafkaService = kafkaService;
        this.topicsFactory = topicsFactory;
        this.kafkaConsumerServiceFactory = kafkaConsumerServiceFactory;
    }

    public void startup(@Observes StartupEvent event) {
        // eager load
    }

    @PostConstruct
    void start() {
        LOGGER.info("Starting up kafka consumers");

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

}
