package me.pcasaes.bbop.service.eventqueue;

import io.quarkus.scheduler.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@ApplicationScoped
public class EventQueueServiceLoader {

    private static final Logger LOGGER = Logger.getLogger(EventQueueServiceLoader.class.getName());

    private Instance<EventQueueConsumerService<?>> eventQueueExecutorServiceFactory;
    private List<EventQueueConsumerService<?>> eventQueueExecutorServiceList = Collections.synchronizedList(new ArrayList<>());
    private Map<Class<?>, EventQueueService> eventQueueServiceMap = new ConcurrentHashMap<>();
    private List<QueueMetric> metrics = Collections.synchronizedList(new ArrayList<>());


    public EventQueueServiceLoader() {
    }

    @Inject
    public EventQueueServiceLoader(
            @Any Instance<EventQueueConsumerService<?>> eventQueueExecutorServiceFactory) {
        this.eventQueueExecutorServiceFactory = eventQueueExecutorServiceFactory;
    }

    public void startup(@Observes @Initialized(ApplicationScoped.class) Object evet) {
        LOGGER.info("Eager load " + this.getClass().getName());
    }

    @PostConstruct
    public void start() {
        eventQueueExecutorServiceFactory
                .forEach(this::init);
    }

    private void init(EventQueueConsumerService eventQueueExecutorService) {
        this.eventQueueExecutorServiceList.add(eventQueueExecutorService);
        QueueMetric metric = new QueueMetric();
        EventQueueService eventQueueService = new EventQueueService(eventQueueExecutorService, metric);
        this.metrics.add(metric);
        eventQueueService.start();
        this.eventQueueServiceMap.put(eventQueueExecutorService.getEventType(), eventQueueService);
    }

    @PreDestroy
    public void stop() {
        this.eventQueueServiceMap.values().forEach(EventQueueService::destroy);
        this.eventQueueExecutorServiceList.forEach(eventQueueExecutorServiceFactory::destroy);
    }

    private Class<?> getPropertyType(InjectionPoint injectionPoint) {
        Type baseType = injectionPoint.getAnnotated().getBaseType();
        if (baseType instanceof Class) {
            return (Class<?>) baseType;
        }

        ParameterizedType parameterizedType = (ParameterizedType) baseType;
        Type type = parameterizedType.getActualTypeArguments()[0];

        /**
         * During programmatic injection we'll have to go one {@link ParameterizedType} deeper.
         * @see javax.enterprise.inject.Instance
         */
        if (type instanceof ParameterizedType) {
            type = ((ParameterizedType) type).getActualTypeArguments()[0];
        }
        return (Class<?>) type;
    }

    @Produces
    public <T> EventQueueService<T> getEventQueueService(InjectionPoint injectionPoint) {
        Class<?> eventType = getPropertyType(injectionPoint);

        if (eventType != null) {
            EventQueueService<T> service = eventQueueServiceMap.get(eventType);
            if (service == null) {
                throw new IllegalStateException("No EventQueuService for type " + eventType);
            }
            return service;
        }
        throw new IllegalStateException("Could not find generic for EventQueuService injection");
    }

    @Scheduled(every = "3s")
    public void reportMetrics() {
        this.metrics.forEach(QueueMetric::report);
    }
}
