package me.pcasaes.hexoids.configuration;


import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import me.pcasaes.hexoids.core.domain.config.Config;
import me.pcasaes.hexoids.core.domain.index.BarrierSpatialIndex;
import me.pcasaes.hexoids.core.domain.index.BarrierSpatialIndexFactory;
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndex;
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndexFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class HexoidConfigurations {

    private static final Logger LOGGER = Logger.getLogger(HexoidConfigurations.class.getName());

    private float inertiaDampenCoefficient;

    private long updateFrequencyInMillis;

    private float minMove;

    private float playerMaxMove;

    private float playerMaxAngleDivisor;

    private long expungeSinceLastSpawnTimeout;

    private String playerResetPosition;

    private int maxBolts;

    private int playerNameLength;

    private int boltMaxDuration;

    private float boltSpeed;

    private float boltCollisionRadius;

    private boolean boltInertiaEnabled;

    private float boltInertiaRejectionScale;

    private float boltInertiaProjectionScale;

    private float boltInertiaNegativeProjectionScale;

    private float playerDestroyedShockwaveDistance;

    private long playerDestroyedShockwaveDuration;

    private float playerDestroyedShockwaveImpulse;

    private float blackholeEventHorizonRadius;

    private float blackholeGravityRadius;

    private float blackholeGravityImpulse;

    private float blackholeDampenFactor;

    private int blackholeGenesisProbabilityFactor;

    private float blackholeTeleportProbability;


    /**
     * This singleton will eager load before all others
     *
     * @param event
     */
    public void startup(@Observes @Priority(Interceptor.Priority.APPLICATION) StartupEvent event) {
        LOGGER.info("Eager load Configuration");
    }

    @PostConstruct
    public void start() {

        if (LOGGER.isLoggable(Level.INFO)) {
            try {
                for (PropertyDescriptor pd : Introspector.getBeanInfo(HexoidConfigurations.class).getPropertyDescriptors()) {
                    if (pd.getReadMethod() != null && pd.getWriteMethod() != null && !"class".equals(pd.getName())) {
                        Annotation[][] annotations = pd.getWriteMethod().getParameterAnnotations();
                        if (annotations.length == 1 && annotations[0].length == 1 &&
                                annotations[0][0] instanceof ConfigProperty configProperty) {
                            LOGGER.info(configProperty.name() + "=" + pd.getReadMethod().invoke(this));
                        }
                    }
                }
            } catch (ReflectiveOperationException | IntrospectionException | RuntimeException ex) {
                LOGGER.warning(() -> "Could not introspect config. " + ex);
            }
        }

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
        Config.get().getPlayerDestroyedShockwave().setDistance(getPlayerDestroyedShockwaveDistance());
        Config.get().getPlayerDestroyedShockwave().setDuration(getPlayerDestroyedShockwaveDuration());
        Config.get().getPlayerDestroyedShockwave().setImpulse(getPlayerDestroyedShockwaveImpulse());
        Config.get().getBlackhole().setDampenFactor(getBlackholeDampenFactor());
        Config.get().getBlackhole().setEventHorizonRadius(getBlackholeEventHorizonRadius());
        Config.get().getBlackhole().setGenesisProbabilityFactor(getBlackholeGenesisProbabilityFactor());
        Config.get().getBlackhole().setGravityImpulse(getBlackholeGravityImpulse());
        Config.get().getBlackhole().setGravityRadius(getBlackholeGravityRadius());
        Config.get().getBlackhole().setTeleportProbability(getBlackholeTeleportProbability());
    }


    public float getInertiaDampenCoefficient() {
        return inertiaDampenCoefficient;
    }

    @Inject
    public void setPlayerSpatialIndex(PlayerSpatialIndex playerSpatialIndex) {
        PlayerSpatialIndexFactory.factory().setPlayerSpatialIndex(playerSpatialIndex);
    }

    @Inject
    public void setBarrierSpatialIndex(BarrierSpatialIndex barrierSpatialIndex) {
        BarrierSpatialIndexFactory.factory().setBarrierSpatialIndex(barrierSpatialIndex);
    }

    @Inject
    public void setInertiaDampenCoefficient(
            @ConfigProperty(
                    name = "hexoids.config.inertia.dampen-coefficient",
                    defaultValue = "-0.001"
            ) float inertiaDampenCoefficient) {
        this.inertiaDampenCoefficient = inertiaDampenCoefficient;
    }

    public long getUpdateFrequencyInMillis() {
        return updateFrequencyInMillis;
    }

    @Inject
    public void setUpdateFrequencyInMillis(
            @ConfigProperty(
                    name = "hexoids.config.update-frequency-in-millis",
                    defaultValue = "50"
            ) long updateFrequencyInMillis) {
        this.updateFrequencyInMillis = updateFrequencyInMillis;
    }

    public float getMinMove() {
        return minMove;
    }

    @Inject
    public void setMinMove(
            @ConfigProperty(
                    name = "hexoids.config.min.move",
                    defaultValue = "0.0001"
            ) float minMove) {
        this.minMove = minMove;
    }

    public float getPlayerMaxMove() {
        return playerMaxMove;
    }

    @Inject
    public void setPlayerMaxMove(
            @ConfigProperty(
                    name = "hexoids.config.player.max.move",
                    defaultValue = "10"
            ) float playerMaxMove) {
        this.playerMaxMove = playerMaxMove;
    }

    public float getPlayerMaxAngleDivisor() {
        return playerMaxAngleDivisor;
    }

    @Inject
    public void setPlayerMaxAngleDivisor(
            @ConfigProperty(
                    name = "hexoids.config.player.max.angle.divisor",
                    defaultValue = "4"
            ) float playerMaxAngleDivisor) {
        this.playerMaxAngleDivisor = playerMaxAngleDivisor;
    }

    public long getExpungeSinceLastSpawnTimeout() {
        return expungeSinceLastSpawnTimeout;
    }

    @Inject

    public void setExpungeSinceLastSpawnTimeout(
            @ConfigProperty(
                    name = "hexoids.config.player.expunge-since-last-spawn-timeout",
                    defaultValue = "60000"
            ) long expungeSinceLastSpawnTimeout) {
        this.expungeSinceLastSpawnTimeout = expungeSinceLastSpawnTimeout;
    }

    public String getPlayerResetPosition() {
        return playerResetPosition;
    }

    @Inject
    public void setPlayerResetPosition(
            @ConfigProperty(
                    name = "hexoids.config.player.reset.position",
                    defaultValue = "rng"
            ) String playerResetPosition) {
        this.playerResetPosition = playerResetPosition;
    }

    public int getMaxBolts() {
        return maxBolts;
    }

    @Inject
    public void setMaxBolts(
            @ConfigProperty(
                    name = "hexoids.config.player.max.bolts",
                    defaultValue = "10"
            ) int maxBolts) {
        this.maxBolts = maxBolts;
    }

    public int getPlayerNameLength() {
        return playerNameLength;
    }

    @Inject
    public void setPlayerNameLength(
            @ConfigProperty(
                    name = "hexoids.config.player.name-length",
                    defaultValue = "7"
            ) int playerNameLength) {
        this.playerNameLength = playerNameLength;
    }

    public int getBoltMaxDuration() {
        return boltMaxDuration;
    }

    @Inject
    public void setBoltMaxDuration(
            @ConfigProperty(
                    name = "hexoids.config.bolt.max.duration",
                    defaultValue = "10000"
            ) int boltMaxDuration) {
        this.boltMaxDuration = boltMaxDuration;
    }

    public float getBoltSpeed() {
        return boltSpeed;
    }

    @Inject
    public void setBoltSpeed(
            @ConfigProperty(
                    name = "hexoids.config.bolt.speed",
                    defaultValue = "0.07"
            ) float boltSpeed) {
        this.boltSpeed = boltSpeed;
    }

    public float getBoltCollisionRadius() {
        return boltCollisionRadius;
    }

    @Inject
    public void setBoltCollisionRadius(
            @ConfigProperty(
                    name = "hexoids.config.bolt.collision.radius",
                    defaultValue = "0.001"
            ) float boltCollisionRadius) {
        this.boltCollisionRadius = boltCollisionRadius;
    }

    public boolean isBoltInertiaEnabled() {
        return boltInertiaEnabled;
    }

    @Inject
    public void setBoltInertiaEnabled(
            @ConfigProperty(
                    name = "hexoids.config.bolt.inertia.enabled",
                    defaultValue = "true"
            ) boolean boltInertiaEnabled) {
        this.boltInertiaEnabled = boltInertiaEnabled;
    }

    public float getBoltInertiaRejectionScale() {
        return boltInertiaRejectionScale;
    }

    @Inject
    public void setBoltInertiaRejectionScale(
            @ConfigProperty(
                    name = "hexoids.config.bolt.inertia.rejection-scale",
                    defaultValue = "0.8"
            ) float boltInertiaRejectionScale) {
        this.boltInertiaRejectionScale = boltInertiaRejectionScale;
    }

    public float getBoltInertiaProjectionScale() {
        return boltInertiaProjectionScale;
    }

    @Inject
    public void setBoltInertiaProjectionScale(
            @ConfigProperty(
                    name = "hexoids.config.bolt.inertia.projection-scale",
                    defaultValue = "0.8"
            ) float boltInertiaProjectionScale) {
        this.boltInertiaProjectionScale = boltInertiaProjectionScale;
    }

    public float getBoltInertiaNegativeProjectionScale() {
        return boltInertiaNegativeProjectionScale;
    }

    @Inject
    public void setBoltInertiaNegativeProjectionScale(
            @ConfigProperty(
                    name = "hexoids.config.bolt.inertia.negative-projection-scale",
                    defaultValue = "0.1"
            ) float boltInertiaNegativeProjectionScale) {
        this.boltInertiaNegativeProjectionScale = boltInertiaNegativeProjectionScale;
    }

    public float getPlayerDestroyedShockwaveDistance() {
        return playerDestroyedShockwaveDistance;
    }

    @Inject
    public void setPlayerDestroyedShockwaveDistance(
            @ConfigProperty(
                    name = "hexoids.config.player.destroyed.shockwave.distance",
                    defaultValue = "0.0408"
            ) float playerDestroyedShockwaveDistance) {
        this.playerDestroyedShockwaveDistance = playerDestroyedShockwaveDistance;
    }

    public long getPlayerDestroyedShockwaveDuration() {
        return playerDestroyedShockwaveDuration;
    }

    @Inject
    public void setPlayerDestroyedShockwaveDuration(
            @ConfigProperty(
                    name = "hexoids.config.player.destroyed.shockwave.duration.ms",
                    defaultValue = "400"
            ) long playerDestroyedShockwaveDuration) {
        this.playerDestroyedShockwaveDuration = playerDestroyedShockwaveDuration;
    }

    public float getPlayerDestroyedShockwaveImpulse() {
        return playerDestroyedShockwaveImpulse;
    }

    @Inject
    public void setPlayerDestroyedShockwaveImpulse(@ConfigProperty(
            name = "hexoids.config.player.destroyed.shockwave.impulse",
            defaultValue = "0.007"
    ) float playerDestroyedShockwaveImpulse) {
        this.playerDestroyedShockwaveImpulse = playerDestroyedShockwaveImpulse;
    }

    public float getBlackholeEventHorizonRadius() {
        return blackholeEventHorizonRadius;
    }

    @Inject
    public void setBlackholeEventHorizonRadius(@ConfigProperty(
            name = "hexoids.config.blackhole.eventhorizon.radius",
            defaultValue = "0.005"
    ) float blackholeEventHorizonRadius) {
        this.blackholeEventHorizonRadius = blackholeEventHorizonRadius;
    }

    public float getBlackholeGravityRadius() {
        return blackholeGravityRadius;
    }

    @Inject
    public void setBlackholeGravityRadius(@ConfigProperty(
            name = "hexoids.config.blackhole.gravity.radius",
            defaultValue = "0.07"
    ) float blackholeGravityRadius) {
        this.blackholeGravityRadius = blackholeGravityRadius;
    }

    public float getBlackholeGravityImpulse() {
        return blackholeGravityImpulse;
    }

    @Inject
    public void setBlackholeGravityImpulse(@ConfigProperty(
            name = "hexoids.config.blackhole.gravity.impulse",
            defaultValue = "0.07"
    ) float blackholeGravityImpulse) {
        this.blackholeGravityImpulse = blackholeGravityImpulse;
    }

    public float getBlackholeDampenFactor() {
        return blackholeDampenFactor;
    }

    @Inject
    public void setBlackholeDampenFactor(@ConfigProperty(
            name = "hexoids.config.blackhole.dampen.factor",
            defaultValue = "5"
    ) float blackholeDampenFactor) {
        this.blackholeDampenFactor = blackholeDampenFactor;
    }

    public int getBlackholeGenesisProbabilityFactor() {
        return blackholeGenesisProbabilityFactor;
    }

    @Inject
    public void setBlackholeGenesisProbabilityFactor(@ConfigProperty(
            name = "hexoids.config.blackhole.genesis.probability.factor",
            defaultValue = "3"
    ) int blackholeGenesisProbabilityFactor) {
        this.blackholeGenesisProbabilityFactor = blackholeGenesisProbabilityFactor;
    }

    public float getBlackholeTeleportProbability() {
        return blackholeTeleportProbability;
    }

    @Inject
    public void setBlackholeTeleportProbability(@ConfigProperty(
            name = "hexoids.config.blackhole.teleport.probability",
            defaultValue = "0.05"
    ) float blackholeTeleportProbability) {
        this.blackholeTeleportProbability = blackholeTeleportProbability;
    }
}
