package me.pcasaes.hexoids.service.eventqueue;

import me.pcasaes.hexoids.model.EntityId;
import me.pcasaes.hexoids.model.Game;
import me.pcasaes.hexoids.service.SessionService;
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
public class ClientBroadcastService implements EventQueueConsumerService<Dto> {

    private final SessionService sessionService;
    private final boolean enabled;
    private final int batchSize;
    private final int batchTimeout;
    private final Dto.Builder dtoBuilder;
    private final Events.Builder eventsBuilder;

    private long flushTimestamp;

    ClientBroadcastService() {
        this.sessionService = null;
        this.enabled = false;
        this.eventsBuilder = null;
        this.dtoBuilder = null;
        this.batchSize = 0;
        this.batchTimeout = 0;
    }


    @Inject
    public ClientBroadcastService(SessionService sessionService,
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

    @Override
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
        long now = Game.get().getClock().getTime();
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

    @Override
    public String getName() {
        return "client-broadcast";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    boolean canFlush() {
        return Game.get().getClock().getTime() - this.flushTimestamp > this.batchTimeout;
    }

    int getBatchTimeout() {
        return batchTimeout;
    }

}
