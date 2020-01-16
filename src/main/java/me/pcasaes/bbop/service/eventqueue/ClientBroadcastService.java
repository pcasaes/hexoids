package me.pcasaes.bbop.service.eventqueue;

import me.pcasaes.bbop.dto.DirectedCommandDto;
import me.pcasaes.bbop.dto.Dto;
import me.pcasaes.bbop.dto.EventDto;
import me.pcasaes.bbop.model.Game;
import me.pcasaes.bbop.service.ConfigurationService;
import me.pcasaes.bbop.service.DtoProcessorService;
import me.pcasaes.bbop.service.SessionService;

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

    private GameLoopService.SleepDto sleepDto = null;

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
            if (dto.getDtoType() == EventDto.DtoType.EVENT_DTO) {
                this.sessionService.broadcast(dtoProcessorService.serializeToString(dto));
            } else if (dto.getDtoType() == DirectedCommandDto.DtoType.DIRECTED_COMMAND_DTO) {
                DirectedCommandDto command = (DirectedCommandDto) dto;
                this.sessionService.direct(command.getPlayerId(), dtoProcessorService.serializeToString(command.getCommand()));
            } else {
                this.sleepDto = (GameLoopService.SleepDto) dto;
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
