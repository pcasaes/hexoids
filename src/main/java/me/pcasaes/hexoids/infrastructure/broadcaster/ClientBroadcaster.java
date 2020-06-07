package me.pcasaes.hexoids.infrastructure.broadcaster;

import me.pcasaes.hexoids.clientinterface.SessionsService;
import me.pcasaes.hexoids.application.clock.GameTime;
import me.pcasaes.hexoids.domain.model.EntityId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pcasaes.hexoids.proto.DirectedCommand;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Events;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.function.LongSupplier;

/**
 * Used to broadcast events to the game clients
 */
@ApplicationScoped
public class ClientBroadcaster {

    private static final String NAME = "client-broadcast";

    private final SessionsService sessionService;
    private final boolean enabled;
    private final int batchSize;
    private final int batchTimeout;
    private final Dto.Builder dtoBuilder;
    private final Events.Builder eventsBuilder;
    private final LongSupplier gameTime;

    private long flushTimestamp;


    @Inject
    public ClientBroadcaster(SessionsService sessionService,
                             @GameTime LongSupplier gameTime,
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
        this.sessionService = sessionService;
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
                this.sessionService.direct(EntityId.of(command.getPlayerId()), dto.toByteArray());
            }
        }
        long now = this.gameTime.getAsLong();
        if (eventsBuilder.getEventsCount() > this.batchSize ||
                now - this.flushTimestamp > this.batchTimeout) {
            flushEvents(now);
        }
    }

    private void flushEvents(long now) {
        if (eventsBuilder.getEventsCount() > 0) {
            this.sessionService.broadcast(dtoBuilder
                    .setEvents(
                            eventsBuilder
                    )
                    .build()
                    .toByteArray());

            dtoBuilder.clearEvents();
            eventsBuilder.clear();
        }
        this.flushTimestamp = now;
    }

    public String getName() {
        return NAME;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean canFlush() {
        return this.gameTime.getAsLong() - this.flushTimestamp > this.batchTimeout;
    }

    public int getBatchTimeout() {
        return batchTimeout;
    }

}
