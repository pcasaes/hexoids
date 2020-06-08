package me.pcasaes.hexoids.entrypoints.web;

import me.pcasaes.hexoids.domain.model.EntityId;
import me.pcasaes.hexoids.domain.service.GameTimeService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pcasaes.hexoids.proto.DirectedCommand;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Events;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Used to broadcast events to the game clients
 */
@ApplicationScoped
public class ClientBroadcaster {

    private static final String NAME = "client-broadcast";

    private final ClientSessions clientSessions;
    private final boolean enabled;
    private final int batchSize;
    private final int batchTimeout;
    private final Dto.Builder dtoBuilder;
    private final Events.Builder eventsBuilder;
    private final GameTimeService gameTime;

    private long flushTimestamp;


    @Inject
    public ClientBroadcaster(ClientSessions clientSessions,
                             GameTimeService gameTime,
                             @ConfigProperty(
                                     name = "hexoids.config.service.client-broadcast.enabled",
                                     defaultValue = "true"
                             ) boolean enabled,
                             @ConfigProperty(
                                     name = "hexoids.config.service.client-broadcast.batch.size",
                                     defaultValue = "64"
                             ) int batchSize,
                             @ConfigProperty(
                                     name = "hexoids.config.service.client-broadcast.batch.timeout",
                                     defaultValue = "20"
                             ) int batchTimeout) {
        this.clientSessions = clientSessions;
        this.gameTime = gameTime;
        this.enabled = enabled;
        this.batchSize = batchSize;
        this.batchTimeout = batchTimeout;
        if (enabled) {
            this.eventsBuilder = Events.newBuilder();
            this.dtoBuilder = Dto.newBuilder();
        } else {
            this.eventsBuilder = null;
            this.dtoBuilder = null;
        }
    }

    public void accept(Dto dto) {
        if (dto != null) {
            if (dto.hasEvent()) {
                eventsBuilder
                        .addEvents(dto.getEvent());
            } else if (dto.hasDirectedCommand()) {
                DirectedCommand command = dto.getDirectedCommand();
                this.clientSessions.direct(EntityId.of(command.getPlayerId()), dto.toByteArray());
            } else if (dto.hasFlush() && canFlush()) {
                flushEvents(this.gameTime.getTime());
                return;
            }
        }
        long now = this.gameTime.getTime();
        if (eventsBuilder.getEventsCount() > this.batchSize ||
                now > this.flushTimestamp) {
            flushEvents(now);
        }
    }

    private void flushEvents(long now) {
        if (eventsBuilder.getEventsCount() > 0) {
            this.clientSessions.broadcast(dtoBuilder
                    .setEvents(
                            eventsBuilder
                    )
                    .build()
                    .toByteArray());

            dtoBuilder.clearEvents();
            eventsBuilder.clear();
        }
        this.flushTimestamp = now + this.batchTimeout;
    }

    public String getName() {
        return NAME;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private boolean canFlush() {
        return this.gameTime.getTime() > this.flushTimestamp;
    }

    public int getBatchTimeout() {
        return batchTimeout;
    }

}
