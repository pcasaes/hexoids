package me.paulo.casaes.bbop.service;


import me.paulo.casaes.bbop.model.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.logging.Logger;

@ApplicationScoped
public class ConfigurationService {

    private static final Logger LOGGER = Logger.getLogger(ConfigurationService.class.getName());

    private String environment;
    private int maxBolts;
    private long boltMaxDuration;
    private float minMove;
    private float playerMaxMove;
    private float playerMaxAngleDivisor;
    private float boltSpeed;
    private float boltCollisionRadius;

    private boolean clientBroadcastUseLinkedList;
    private int clientBroadcastMaxSizeExponent;

    private boolean domainEventUseLinkedList;
    private int domainEventMaxSizeExponent;

    private boolean gameLoopUseLinkedList;
    private int gameLoopMaxSizeExponent;


    /**
     * Required for CDI Normal Scoped beans
     */
    ConfigurationService() {
    }

    @Inject
    public ConfigurationService(
            @ConfigProperty(
                    name = "bbop.config.environment",
                    defaultValue = "PRODUCTION"
            ) String environment,

            @ConfigProperty(
                    name = "bbop.config.player.max.bolts",
                    defaultValue = "10"
            ) int maxBolts,

            @ConfigProperty(
                    name = "bbop.config.min.move",
                    defaultValue = "0.000000001"
            ) float minMove,

            @ConfigProperty(
                    name = "bbop.config.player.max.move",
                    defaultValue = "10"
            ) float playerMaxMove,

            @ConfigProperty(
                    name = "bbop.config.player.max.angle.divisor",
                    defaultValue = "4"
            ) float playerMaxAngleDivisor,

            @ConfigProperty(
                    name = "bbop.config.bolt.max.duration",
                    defaultValue = "10000"
            ) long boltMaxDuration,

            @ConfigProperty(
                    name = "bbop.config.bolt.speed",
                    defaultValue = "0.07"
            ) float boltSpeed,

            @ConfigProperty(
                    name = "bbop.config.bolt.collision.radius",
                    defaultValue = "0.001"
            ) float boltCollisionRadius,

            @ConfigProperty(
                    name = "bbop.config.service.domain.event.eventqueue.linkedlist",
                    defaultValue = "false"
            ) boolean domainEventUseLinkedList,

            @ConfigProperty(
                    name = "bbop.config.service.domain.event.eventqueue.exponent",
                    defaultValue = "17"
            ) int domainEventMaxSizeExponent,

            @ConfigProperty(
                    name = "bbop.config.service.client.broadcast.eventqueue.linkedlist",
                    defaultValue = "false"
            ) boolean clientBroadcastUseLinkedList,

            @ConfigProperty(
                    name = "bbop.config.service.client.broadcast.eventqueue.exponent",
                    defaultValue = "17"
            ) int clientBroadcastMaxSizeExponent,

            @ConfigProperty(
                    name = "bbop.config.service.game.loop.eventqueue.linkedlist",
                    defaultValue = "false"
            ) boolean gameLoopUseLinkedList,

            @ConfigProperty(
                    name = "bbop.config.service.game.loop.eventqueue.exponent",
                    defaultValue = "17"
            ) int gameLoopMaxSizeExponent
    ) {
        this.environment = environment;
        this.playerMaxMove = playerMaxMove;
        this.minMove = minMove;
        this.playerMaxAngleDivisor = playerMaxAngleDivisor;
        this.maxBolts = maxBolts;
        this.boltMaxDuration = boltMaxDuration;
        this.boltSpeed = boltSpeed;
        this.boltCollisionRadius = boltCollisionRadius;

        this.clientBroadcastUseLinkedList = clientBroadcastUseLinkedList;
        this.clientBroadcastMaxSizeExponent = clientBroadcastMaxSizeExponent;

        this.domainEventUseLinkedList = domainEventUseLinkedList;
        this.domainEventMaxSizeExponent = domainEventMaxSizeExponent;

        this.gameLoopUseLinkedList = gameLoopUseLinkedList;
        this.gameLoopMaxSizeExponent = gameLoopMaxSizeExponent;
    }

    public void startup(@Observes @Initialized(ApplicationScoped.class) Object env) {
        LOGGER.info("Eager load Configuration");
    }

    @PostConstruct
    public void start() {
        LOGGER.info("bbop.config.environment=" + getEnvironment());
        LOGGER.info("bbop.config.min.min=" + getMinMove());
        LOGGER.info("bbop.config.player.max.move=" + getPlayerMaxMove());
        LOGGER.info("bbop.config.player.max.bolts=" + getMaxBolts());
        LOGGER.info("bbop.config.player.max.angle.divisors=" + getPlayerMaxAngleDivisor());
        LOGGER.info("bbop.config.bolt.max.duration=" + getBoltMaxDuration());
        LOGGER.info("bbop.config.bolt.speed=" + getBoltSpeed());
        LOGGER.info("bbop.config.bolt.collision.radius=" + getBoltCollisionRadius());
        LOGGER.info("bbop.config.service.client.broadcast.eventqueue.linkedlist=" + isClientBroadcastUseLinkedList());
        LOGGER.info("bbop.config.service.client.broadcast.eventqueue.exponent=" + getClientBroadcastMaxSizeExponent());
        LOGGER.info("bbop.config.service.domain.event.eventqueue.linkedlist=" + isDomainEventUseLinkedList());
        LOGGER.info("bbop.config.service.domain.event.eventqueue.exponent=" + getDomainEventMaxSizeExponent());
        LOGGER.info("bbop.config.service.game.loop.eventqueue.linkedlist=" + isGameLoopUseLinkedList());
        LOGGER.info("bbop.config.service.game.loop.eventqueue.exponent=" + getGameLoopMaxSizeExponent());

        Config.get().setEnv(getEnvironment());
        Config.get().setMaxBolts(getMaxBolts());
        Config.get().setMinMove(getMinMove());
        Config.get().setPlayerMaxMove(getPlayerMaxMove());
        Config.get().setPlayerMaxAngleDivisor(getPlayerMaxAngleDivisor());
        Config.get().setBoltMaxDuration(getBoltMaxDuration());
        Config.get().setBoltSpeed(getBoltSpeed());
        Config.get().setBoltCollisionRadius(getBoltCollisionRadius());
    }


    public String getEnvironment() {
        return environment;
    }

    public float getMinMove() {
        return minMove;
    }

    public float getPlayerMaxMove() {
        return playerMaxMove;
    }

    public float getPlayerMaxAngleDivisor() {
        return playerMaxAngleDivisor;
    }

    public int getMaxBolts() {
        return maxBolts;
    }

    public long getBoltMaxDuration() {
        return boltMaxDuration;
    }

    public float getBoltSpeed() {
        return boltSpeed;
    }

    public float getBoltCollisionRadius() {
        return boltCollisionRadius;
    }

    public boolean isClientBroadcastUseLinkedList() {
        return clientBroadcastUseLinkedList;
    }

    public int getClientBroadcastMaxSizeExponent() {
        return clientBroadcastMaxSizeExponent;
    }

    public boolean isDomainEventUseLinkedList() {
        return domainEventUseLinkedList;
    }

    public int getDomainEventMaxSizeExponent() {
        return domainEventMaxSizeExponent;
    }

    public boolean isGameLoopUseLinkedList() {
        return gameLoopUseLinkedList;
    }

    public int getGameLoopMaxSizeExponent() {
        return gameLoopMaxSizeExponent;
    }
}
