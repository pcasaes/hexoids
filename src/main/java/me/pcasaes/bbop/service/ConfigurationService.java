package me.pcasaes.bbop.service;


import io.quarkus.runtime.StartupEvent;
import me.pcasaes.bbop.model.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.logging.Logger;

@ApplicationScoped
public class ConfigurationService {

    private static final Logger LOGGER = Logger.getLogger(ConfigurationService.class.getName());

    @Inject
    @ConfigProperty(
            name = "bbop.config.inertia.dampen-coefficient",
            defaultValue = "-0.001"
    )
    private float inertiaDampenCoefficient;

    @Inject
    @ConfigProperty(
            name = "bbop.config.update-frequency-in-millis",
            defaultValue = "50"
    )
    private long updateFrequencyInMillis;


    @Inject
    @ConfigProperty(
            name = "bbop.config.min.move",
            defaultValue = "0.000000001"
    )
    private float minMove;

    @Inject
    @ConfigProperty(
            name = "bbop.config.player.max.move",
            defaultValue = "10"
    )
    private float playerMaxMove;

    @Inject
    @ConfigProperty(
            name = "bbop.config.player.max.angle.divisor",
            defaultValue = "4"
    )
    private float playerMaxAngleDivisor;

    @Inject
    @ConfigProperty(
            name = "bbop.config.player.expungeSinceLastSpawnTimeout",
            defaultValue = "60000"
    )
    private long expungeSinceLastSpawnTimeout;

    @Inject
    @ConfigProperty(
            name = "bbop.config.player.reset.position",
            defaultValue = "rng"
    )
    private String playerResetPosition;

    @Inject
    @ConfigProperty(
            name = "bbop.config.player.max.bolts",
            defaultValue = "10"
    )
    private int maxBolts;

    @Inject
    @ConfigProperty(
            name = "bbop.config.player.name-length",
            defaultValue = "7"
    )
    private int playerNameLength;

    @Inject
    @ConfigProperty(
            name = "bbop.config.bolt.max.duration",
            defaultValue = "10000"
    )
    private long boltMaxDuration;

    @Inject
    @ConfigProperty(
            name = "bbop.config.bolt.speed",
            defaultValue = "0.07"
    )
    private float boltSpeed;

    @Inject
    @ConfigProperty(
            name = "bbop.config.bolt.collision.radius",
            defaultValue = "0.001"
    )
    private float boltCollisionRadius;

    @Inject
    @ConfigProperty(
            name = "bbop.config.bolt.inertia.enabled",
            defaultValue = "true"
    )
    private boolean boltInertiaEnabled;

    @Inject
    @ConfigProperty(
            name = "bbop.config.bolt.inertia.rejection-scale",
            defaultValue = "0.8"
    )
    private float boltInertiaRejectionScale;

    @Inject
    @ConfigProperty(
            name = "bbop.config.bolt.inertia.projection-scale",
            defaultValue = "0.8"
    )
    private float boltInertiaProjectionScale;

    @Inject
    @ConfigProperty(
            name = "bbop.config.bolt.inertia.negative-projection-scale",
            defaultValue = "0.1"
    )
    private float boltInertiaNegativeProjectionScale;

    @Inject
    @ConfigProperty(
            name = "bbop.config.service.client-broadcast.event-queue.linked-list",
            defaultValue = "false"
    )
    private boolean clientBroadcastUseLinkedList;

    @Inject
    @ConfigProperty(
            name = "bbop.config.service.client-broadcast.event-queue.exponent",
            defaultValue = "17"
    )
    private int clientBroadcastMaxSizeExponent;

    @Inject
    @ConfigProperty(
            name = "bbop.config.service.domain-event.event-queue.linked-list",
            defaultValue = "false"
    )
    private boolean domainEventUseLinkedList;

    @Inject
    @ConfigProperty(
            name = "bbop.config.service.domain-event.event-queue.exponent",
            defaultValue = "17"
    )
    private int domainEventMaxSizeExponent;

    @Inject
    @ConfigProperty(
            name = "bbop.config.service.game.loop.eventqueue.linkedlist",
            defaultValue = "false"
    )
    private boolean gameLoopUseLinkedList;

    @Inject
    @ConfigProperty(
            name = "bbop.config.service.game.loop.eventqueue.exponent",
            defaultValue = "17"
    )
    private int gameLoopMaxSizeExponent;


    public void startup(@Observes StartupEvent event) {
        LOGGER.info("Eager load Configuration");
    }

    @PostConstruct
    public void start() {
        LOGGER.info("bbop.config.inertia.dampen-coefficients=" + getInertiaDampenCoefficient());
        LOGGER.info("bbop.config.update-frequency-in-millis=" + getUpdateFrequencyInMillis());
        LOGGER.info("bbop.config.min.min=" + getMinMove());
        LOGGER.info("bbop.config.player.expungeSinceLastSpawnTimeout=" + getExpungeSinceLastSpawnTimeout());
        LOGGER.info("bbop.config.player.name-length=" + getPlayerNameLength());
        LOGGER.info("bbop.config.player.max.move=" + getPlayerMaxMove());
        LOGGER.info("bbop.config.player.max.bolts=" + getMaxBolts());
        LOGGER.info("bbop.config.player.max.angle.divisors=" + getPlayerMaxAngleDivisor());
        LOGGER.info("bbop.config.player.reset.position=" + getPlayerResetPosition());
        LOGGER.info("bbop.config.bolt.max.duration=" + getBoltMaxDuration());
        LOGGER.info("bbop.config.bolt.speed=" + getBoltSpeed());
        LOGGER.info("bbop.config.bolt.collision.radius=" + getBoltCollisionRadius());
        LOGGER.info("bbop.config.bolt.inertia.enabled=" + isBoltInertiaEnabled());
        LOGGER.info("bbop.config.bolt.inertia.rejection-scale=" + getBoltInertiaRejectionScale());
        LOGGER.info("bbop.config.bolt.inertia.projection-scale=" + getBoltInertiaProjectionScale());
        LOGGER.info("bbop.config.bolt.inertia.negative-projection-scale=" + getBoltInertiaNegativeProjectionScale());
        LOGGER.info("bbop.config.service.client-broadcast.event-queue.linked-list=" + isClientBroadcastUseLinkedList());
        LOGGER.info("bbop.config.service.client-broadcast.event-queue.exponent=" + getClientBroadcastMaxSizeExponent());
        LOGGER.info("bbop.config.service.domain-event.event-queue.linked-list=" + isDomainEventUseLinkedList());
        LOGGER.info("bbop.config.service.domain-event.event-queue.exponent=" + getDomainEventMaxSizeExponent());
        LOGGER.info("bbop.config.service.game.loop.eventqueue.linkedlist=" + isGameLoopUseLinkedList());
        LOGGER.info("bbop.config.service.game.loop.eventqueue.exponent=" + getGameLoopMaxSizeExponent());

        Config.get().setUpdateFrequencyInMillis(getUpdateFrequencyInMillis());
        Config.get().setInertiaDampenCoefficient(getInertiaDampenCoefficient());
        Config.get().setMaxBolts(getMaxBolts());
        Config.get().setMinMove(getMinMove());
        Config.get().setExpungeSinceLastSpawnTimeout(getExpungeSinceLastSpawnTimeout());
        Config.get().setPlayerNameLength(getPlayerNameLength());
        Config.get().setPlayerMaxMove(getPlayerMaxMove());
        Config.get().setPlayerMaxAngleDivisor(getPlayerMaxAngleDivisor());
        Config.get().setPlayerResetPosition(getPlayerResetPosition());
        Config.get().setBoltMaxDuration(getBoltMaxDuration());
        Config.get().setBoltSpeed(getBoltSpeed());
        Config.get().setBoltCollisionRadius(getBoltCollisionRadius());
        Config.get().setBoltInertiaEnabled(isBoltInertiaEnabled());
        Config.get().setBoltInertiaRejectionScale(getBoltInertiaRejectionScale());
        Config.get().setBoltInertiaProjectionScale(getBoltInertiaProjectionScale());
        Config.get().setBoltInertiaNegativeProjectionScale(getBoltInertiaNegativeProjectionScale());
    }


    public float getInertiaDampenCoefficient() {
        return inertiaDampenCoefficient;
    }

    public long getUpdateFrequencyInMillis() {
        return updateFrequencyInMillis;
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

    public String getPlayerResetPosition() {
        return playerResetPosition;
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

    public boolean isBoltInertiaEnabled() {
        return boltInertiaEnabled;
    }

    public float getBoltInertiaRejectionScale() {
        return boltInertiaRejectionScale;
    }

    public float getBoltInertiaProjectionScale() {
        return boltInertiaProjectionScale;
    }

    public float getBoltInertiaNegativeProjectionScale() {
        return boltInertiaNegativeProjectionScale;
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

    public long getExpungeSinceLastSpawnTimeout() {
        return expungeSinceLastSpawnTimeout;
    }

    public int getPlayerNameLength() {
        return playerNameLength;
    }
}
