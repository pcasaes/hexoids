package me.pcasaes.hexoids.service.eventqueue;

import me.pcasaes.hexoids.model.EntityId;
import me.pcasaes.hexoids.model.Game;
import me.pcasaes.hexoids.service.ConfigurationService;
import me.pcasaes.hexoids.service.SessionService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pcasaes.hexoids.proto.DirectedCommand;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Events;
import pcasaes.hexoids.proto.Sleep;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Used to broadcast events to the game clients
 */
@ApplicationScoped
public class ClientBroadcastService implements EventQueueConsumerService<ClientBroadcastService.ClientEvent> {

    private final SessionService sessionService;
    private final ConfigurationService configurationService;
    private final boolean enabled;
    private final int batchSize;
    private final int batchTimeout;
    private final long sleepOnEmpty;
    private final Dto.Builder dtoBuilder;
    private final Events.Builder eventsBuilder;

    private Sleep sleepDto = null;
    private long flushTimestamp;

    ClientBroadcastService() {
        this.sessionService = null;
        this.configurationService = null;
        this.enabled = false;
        this.eventsBuilder = null;
        this.dtoBuilder = null;
        this.batchSize = 0;
        this.batchTimeout = 0;
        this.sleepOnEmpty = 0;
    }


    @Inject
    public ClientBroadcastService(SessionService sessionService,
                                  ConfigurationService configurationService,
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
                                  ) int batchTimeout,
                                  @ConfigProperty(
                                          name = "hexoids.config.service.client-broadcast.event-queue.sleep-on-empty",
                                          defaultValue = "5"
                                  ) long sleepOnEmpty) {
        this.sessionService = sessionService;
        this.configurationService = configurationService;
        this.enabled = enabled;
        this.batchSize = batchSize;
        this.batchTimeout = batchTimeout;
        this.sleepOnEmpty = sleepOnEmpty;
        if (enabled) {
            this.eventsBuilder = Events.newBuilder();
            this.dtoBuilder = Dto.newBuilder();
        } else {
            this.eventsBuilder = null;
            this.dtoBuilder = null;
        }
    }

    @Override
    public void accept(ClientEvent event) {
        if (event != null) {
            Dto dto = event.getDto();
            if (dto.hasSleep()) {
                this.sleepDto = dto.getSleep();
            } else if (dto.hasEvent()) {
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
    public void empty() {
        flushEvents(Game.get().getClock().getTime());
    }

    @Override
    public long getWaitTime() {
        if (this.sleepDto == null) {
            return this.sleepOnEmpty;
        }
        long waitTime = sleepDto.getSleepUntil() - Game.get().getClock().getTime();
        this.sleepDto = null;

        return waitTime;
    }

    @Override
    public boolean useLinkedList() {
        return configurationService.isClientBroadcastUseLinkedList();
    }

    @Override
    public boolean useSingleProducer() {
        return true;
    }

    @Override
    public int getMaxSizeExponent() {
        return configurationService.getClientBroadcastMaxSizeExponent();
    }

    @Override
    public String getName() {
        return ClientBroadcastService.class.getSimpleName();
    }

    @Override
    public Class<?> getEventType() {
        return ClientEvent.class;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public static class ClientEvent {
        private Dto dto;

        private ClientEvent(Dto dto) {
            this.dto = dto;
        }

        public static ClientEvent of(Dto dto) {
            return new ClientEvent(dto);
        }

        private Dto getDto() {
            return dto;
        }
    }
}
