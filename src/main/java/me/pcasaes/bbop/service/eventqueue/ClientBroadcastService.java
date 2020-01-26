package me.pcasaes.bbop.service.eventqueue;

import me.pcasaes.bbop.model.EntityId;
import me.pcasaes.bbop.model.Game;
import me.pcasaes.bbop.service.ConfigurationService;
import me.pcasaes.bbop.service.SessionService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pcasaes.bbop.proto.DirectedCommand;
import pcasaes.bbop.proto.Dto;
import pcasaes.bbop.proto.Sleep;

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

    private Sleep sleepDto = null;

    ClientBroadcastService() {
        this.sessionService = null;
        this.configurationService = null;
        this.enabled = false;
    }


    @Inject
    public ClientBroadcastService(SessionService sessionService,
                                  ConfigurationService configurationService,
                                  @ConfigProperty(
                                          name = "bbop.config.service.client.broadcast.enabled",
                                          defaultValue = "true"
                                  ) boolean enabled) {
        this.sessionService = sessionService;
        this.configurationService = configurationService;
        this.enabled = enabled;
    }

    @Override
    public void accept(ClientEvent event) {
        if (event != null) {
            Dto dto = event.getDto();
            if (dto.hasSleep()) {
                this.sleepDto = dto.getSleep();
            } else if (dto.hasEvent()) {
                this.sessionService.broadcast(dto.toByteArray());
            } else if (dto.hasDirectedCommand()) {
                DirectedCommand command = dto.getDirectedCommand();
                this.sessionService.direct(EntityId.of(command.getPlayerId()), dto.toByteArray());
            }
        }
    }

    @Override
    public void empty() {
        // do nothing on empty
    }

    @Override
    public long getWaitTime() {
        if (this.sleepDto == null) {
            return 0L;
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
