package me.pcasaes.hexoids.model;

import pcasaes.hexoids.proto.BoltExhaustedEventDto;
import pcasaes.hexoids.proto.BoltFiredEventDto;
import pcasaes.hexoids.proto.DirectedCommand;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Event;
import pcasaes.hexoids.proto.GUID;
import pcasaes.hexoids.proto.PlayerDestroyedEventDto;
import pcasaes.hexoids.proto.PlayerDto;
import pcasaes.hexoids.proto.PlayerJoinedEventDto;
import pcasaes.hexoids.proto.PlayerLeftEventDto;
import pcasaes.hexoids.proto.PlayerMovedEventDto;
import pcasaes.hexoids.proto.PlayerScoreIncreasedEventDto;
import pcasaes.hexoids.proto.PlayerScoreUpdateCommandDto;
import pcasaes.hexoids.proto.PlayerScoreUpdatedEventDto;
import pcasaes.hexoids.proto.PlayerSpawnedEventDto;
import pcasaes.hexoids.proto.PlayersListCommandDto;
import pcasaes.hexoids.proto.RequestCommand;
import pcasaes.hexoids.proto.ScoreBoardUpdatedEventDto;
import pcasaes.hexoids.proto.ScoreEntry;

import java.util.function.Consumer;

public class DtoUtils {

    private DtoUtils() {

    }

    static final ThreadLocal<Dto.Builder> DTO_THREAD_SAFE_BUILDER = ThreadLocal
            .withInitial(Dto::newBuilder);

    public static final ThreadLocal<Event.Builder> EVENT_THREAD_SAFE_BUILDER = ThreadLocal
            .withInitial(Event::newBuilder);

    static final ThreadLocal<DirectedCommand.Builder> DIRECTED_COMMAND_THREAD_SAFE_BUILDER = ThreadLocal
            .withInitial(DirectedCommand::newBuilder);

    static final ThreadLocal<PlayersListCommandDto.Builder> PLAYERS_LIST_THREAD_SAFE_BUILDER = ThreadLocal
            .withInitial(PlayersListCommandDto::newBuilder);

    static final ThreadLocal<PlayerDto.Builder> PLAYER_THREAD_SAFE_BUILDER = ThreadLocal
            .withInitial(PlayerDto::newBuilder);

    public static final ThreadLocal<RequestCommand.Builder> REQUEST_COMMAND_THREAD_SAFE_BUILDER = ThreadLocal
            .withInitial(RequestCommand::newBuilder);


    static final Dto.Builder DTO_BUILDER = Dto.newBuilder();

    static final Event.Builder EVENT_BUILDER = Event.newBuilder();

    static final DirectedCommand.Builder DIRECTED_COMMAND_BUILDER = DirectedCommand.newBuilder();

    static final PlayerSpawnedEventDto.Builder PLAYER_SPAWNED_BUILDER = PlayerSpawnedEventDto.newBuilder();

    static final PlayerMovedEventDto.Builder PLAYER_MOVED_BUILDER = PlayerMovedEventDto.newBuilder();

    static final PlayerLeftEventDto.Builder PLAYER_LEFT_BUILDER = PlayerLeftEventDto.newBuilder();

    static final PlayerDestroyedEventDto.Builder PLAYER_DESTROYED_BUILDER = PlayerDestroyedEventDto.newBuilder();

    static final PlayerJoinedEventDto.Builder PLAYER_JOINED_BUILDER = PlayerJoinedEventDto.newBuilder();

    static final BoltFiredEventDto.Builder BOLT_FIRED_BUILDER = BoltFiredEventDto.newBuilder();

    static final BoltExhaustedEventDto.Builder BOLT_EXHAUSTED_BUILDER = BoltExhaustedEventDto.newBuilder();

    static final PlayerScoreIncreasedEventDto.Builder PLAYER_SCORE_INCREASED_BUILDER = PlayerScoreIncreasedEventDto.newBuilder();

    static final PlayerScoreUpdatedEventDto.Builder PLAYER_SCORE_UPDATED_BUILDER = PlayerScoreUpdatedEventDto.newBuilder();

    static final PlayerScoreUpdateCommandDto.Builder PLAYER_SCORE_UPDATE_COMMAND_BUILDER = PlayerScoreUpdateCommandDto.newBuilder();

    static final ScoreBoardUpdatedEventDto.Builder SCORE_BOARD_UPDATE_BUILDER = ScoreBoardUpdatedEventDto.newBuilder();

    static final ScoreEntry.Builder SCORE_BOARD_ENTRY_BUILDER = ScoreEntry.newBuilder();


    static Dto newDtoEvent(Consumer<Event.Builder> event) {
        event.accept(EVENT_BUILDER.clear());
        return DTO_BUILDER
                .clear()
                .setEvent(EVENT_BUILDER)
                .build();
    }

    static Dto newDtoDirectedCommand(GUID playerId, Consumer<DirectedCommand.Builder> event) {
        event.accept(
                DIRECTED_COMMAND_BUILDER
                        .clear()
                        .setPlayerId(playerId)
        );
        return DTO_BUILDER
                .clear()
                .setDirectedCommand(DIRECTED_COMMAND_BUILDER)
                .build();
    }

    static Event.Builder newEvent() {
        return EVENT_BUILDER
                .clear();
    }
}
