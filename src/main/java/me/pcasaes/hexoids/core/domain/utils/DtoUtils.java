package me.pcasaes.hexoids.core.domain.utils;

import pcasaes.hexoids.proto.BoltExhaustedEventDto;
import pcasaes.hexoids.proto.BoltFiredEventDto;
import pcasaes.hexoids.proto.BoltsAvailableCommandDto;
import pcasaes.hexoids.proto.ClockSync;
import pcasaes.hexoids.proto.DirectedCommand;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Event;
import pcasaes.hexoids.proto.GUID;
import pcasaes.hexoids.proto.LiveBoltListCommandDto;
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

    public static final ThreadLocal<Dto.Builder> DTO_THREAD_SAFE_BUILDER = ThreadLocal
            .withInitial(Dto::newBuilder);

    public static final ThreadLocal<Event.Builder> EVENT_THREAD_SAFE_BUILDER = ThreadLocal
            .withInitial(Event::newBuilder);

    public static final ThreadLocal<RequestCommand.Builder> REQUEST_COMMAND_THREAD_SAFE_BUILDER = ThreadLocal
            .withInitial(RequestCommand::newBuilder);

    public static final ThreadLocal<ClockSync.Builder> CLOCK_SYNC_THREAD_SAFE_BUILDER = ThreadLocal
            .withInitial(ClockSync::newBuilder);


    public static final Dto.Builder DTO_BUILDER = Dto.newBuilder();

    public static final Event.Builder EVENT_BUILDER = Event.newBuilder();

    public static final PlayerSpawnedEventDto.Builder PLAYER_SPAWNED_BUILDER = PlayerSpawnedEventDto.newBuilder();

    public static final PlayerMovedEventDto.Builder PLAYER_MOVED_BUILDER = PlayerMovedEventDto.newBuilder();

    public static final PlayerLeftEventDto.Builder PLAYER_LEFT_BUILDER = PlayerLeftEventDto.newBuilder();

    public static final PlayerDestroyedEventDto.Builder PLAYER_DESTROYED_BUILDER = PlayerDestroyedEventDto.newBuilder();

    public static final PlayerJoinedEventDto.Builder PLAYER_JOINED_BUILDER = PlayerJoinedEventDto.newBuilder();

    public static final BoltFiredEventDto.Builder BOLT_FIRED_BUILDER = BoltFiredEventDto.newBuilder();

    public static final BoltExhaustedEventDto.Builder BOLT_EXHAUSTED_BUILDER = BoltExhaustedEventDto.newBuilder();

    public static final PlayerScoreIncreasedEventDto.Builder PLAYER_SCORE_INCREASED_BUILDER = PlayerScoreIncreasedEventDto.newBuilder();

    public static final PlayerScoreUpdatedEventDto.Builder PLAYER_SCORE_UPDATED_BUILDER = PlayerScoreUpdatedEventDto.newBuilder();

    public static final PlayerScoreUpdateCommandDto.Builder PLAYER_SCORE_UPDATE_COMMAND_BUILDER = PlayerScoreUpdateCommandDto.newBuilder();

    public static final ScoreBoardUpdatedEventDto.Builder SCORE_BOARD_UPDATE_BUILDER = ScoreBoardUpdatedEventDto.newBuilder();

    public static final ScoreEntry.Builder SCORE_BOARD_ENTRY_BUILDER = ScoreEntry.newBuilder();


    public static final DirectedCommand.Builder DIRECTED_COMMAND_BUILDER = DirectedCommand.newBuilder();

    public static final PlayersListCommandDto.Builder PLAYERS_LIST_BUILDER = PlayersListCommandDto.newBuilder();

    public static final BoltsAvailableCommandDto.Builder BOLTS_AVAILABLE_BUILDER = BoltsAvailableCommandDto.newBuilder();

    public static final PlayerDto.Builder PLAYER_BUILDER = PlayerDto.newBuilder();

    public static final LiveBoltListCommandDto.Builder LIVE_BOLTS_LIST_BUILDER = LiveBoltListCommandDto.newBuilder();


    public static Dto newDtoEvent(Consumer<Event.Builder> event) {
        event.accept(EVENT_BUILDER.clear());
        return DTO_BUILDER
                .clear()
                .setEvent(EVENT_BUILDER)
                .build();
    }

    public static Dto newDtoDirectedCommand(GUID playerId, Consumer<DirectedCommand.Builder> event) {
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

    public static Event.Builder newEvent() {
        return EVENT_BUILDER
                .clear();
    }
}
