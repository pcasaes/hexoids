package me.paulo.casaes.bbop.service.eventqueue;

import me.paulo.casaes.bbop.dto.DirectedCommandDto;
import me.paulo.casaes.bbop.dto.Dto;
import me.paulo.casaes.bbop.dto.EventDto;
import me.paulo.casaes.bbop.service.ConfigurationService;
import me.paulo.casaes.bbop.service.DtoProcessorService;
import me.paulo.casaes.bbop.service.SessionService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Used to broadcast events to the game clients
 */
@ApplicationScoped
public class ClientBroadcastService implements EventQueueConsumerService<ClientBroadcastService.ClientEvent> {

    private final SessionService sessionService;
    private final DtoProcessorService dtoProcessorService;
    private final ConfigurationService configurationService;

    ClientBroadcastService() {
        this.sessionService = null;
        this.dtoProcessorService = null;
        this.configurationService = null;
    }

    @Inject
    public ClientBroadcastService(SessionService sessionService,
                                  DtoProcessorService dtoProcessorService,
                                  ConfigurationService configurationService) {
        this.sessionService = sessionService;
        this.dtoProcessorService = dtoProcessorService;
        this.configurationService = configurationService;
    }

    @Override
    public void accept(ClientEvent event) {
        if (event != null) {
            Dto dto = event.getDto();
            if (dto instanceof EventDto) {
                this.sessionService.broadcast(dtoProcessorService.serializeToString(dto));
            } else if (dto != null) {
                DirectedCommandDto command = (DirectedCommandDto) dto;
                this.sessionService.direct(command.getPlayerId(), dtoProcessorService.serializeToString(command.getCommand()));
            }
        }
    }

    @Override
    public void empty() {
        // do nothing on empty
    }

    @Override
    public long getWaitTime() {
        return 20;
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
