package me.pcasaes.bbop.service.eventqueue;

import me.pcasaes.bbop.dto.DirectedCommandDto;
import me.pcasaes.bbop.dto.Dto;
import me.pcasaes.bbop.dto.EventDto;
import me.pcasaes.bbop.model.Game;
import me.pcasaes.bbop.service.ConfigurationService;
import me.pcasaes.bbop.service.DtoProcessorService;
import me.pcasaes.bbop.service.SessionService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to broadcast events to the game clients
 */
@ApplicationScoped
public class ClientBroadcastService implements EventQueueConsumerService<ClientBroadcastService.ClientEvent>, Closeable {

    private static final Logger LOGGER = Logger.getLogger(ClientBroadcastService.class.getName());

    private final SessionService sessionService;
    private final ConfigurationService configurationService;
    private final boolean enabled;

    private GameLoopService.SleepDto sleepDto = null;

    private final DtoProcessorService.JsonWriter jsonWriter;

    ClientBroadcastService() {
        this.sessionService = null;
        this.configurationService = null;
        this.jsonWriter = null;
        this.enabled = false;
    }


    @Inject
    public ClientBroadcastService(SessionService sessionService,
                                  DtoProcessorService dtoProcessorService,
                                  ConfigurationService configurationService,
                                  @ConfigProperty(
                                          name = "bbop.config.service.client.broadcast.enabled",
                                          defaultValue = "true"
                                  ) boolean enabled) {
        this.sessionService = sessionService;
        this.configurationService = configurationService;
        this.enabled = enabled;
        this.jsonWriter = dtoProcessorService.createJsonWriter();
    }

    @PreDestroy
    @Override
    public void close() {
        close(jsonWriter);
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    private String serialize(Object value) {
        try {
            return jsonWriter.writeValue(value);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }


    @Override
    public void accept(ClientEvent event) {
        if (event != null) {
            Dto dto = event.getDto();
            if (dto.getDtoType() == GameLoopService.SleepDto.DtoType.SLEEP_DTO) {
                this.sleepDto = (GameLoopService.SleepDto) dto;
            } else if (dto.getDtoType() == EventDto.DtoType.EVENT_DTO) {
                this.sessionService.broadcast(serialize(dto));
            } else if (dto.getDtoType() == DirectedCommandDto.DtoType.DIRECTED_COMMAND_DTO) {
                DirectedCommandDto command = (DirectedCommandDto) dto;
                this.sessionService.direct(command.getPlayerId(), serialize(command.getCommand()));
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
