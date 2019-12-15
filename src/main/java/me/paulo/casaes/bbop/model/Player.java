package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.EventDto;
import me.paulo.casaes.bbop.dto.PlayerDestroyedEventDto;
import me.paulo.casaes.bbop.dto.PlayerDto;
import me.paulo.casaes.bbop.dto.PlayerJoinedEventDto;
import me.paulo.casaes.bbop.dto.PlayerLeftEventDto;
import me.paulo.casaes.bbop.dto.PlayerMovedEventDto;
import me.paulo.casaes.bbop.dto.PlayersListCommandDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class Player {

    private static final float MIN_MOVE = 100f * Float.MIN_VALUE;

    private static final Random RNG = new Random();

    private static final Map<String, Player> PLAYERS = new HashMap<>();

    private final String id;

    private final int ship;

    private float x = 0f;

    private float y = 0f;

    private float angle = 0f;

    private float currentSpeed = 0f;

    private Collection<Bolt> firedBolts = new ArrayList<>();

    private Player(String id) {
        this.id = id;

        this.ship = RNG.nextInt(6);
    }

    public void fire() {
        firedBolts.removeIf(Bolt::isExhausted);
        if (firedBolts.size() < Config.get().getMaxBolts()) {
            firedBolts.add(Bolt.fire(this.id, this.x, this.y, this.angle, this.currentSpeed));
        }
    }

    Iterable<Bolt> getActiveBolts() {
        return this.firedBolts
                .stream()
                .filter(Bolt::isActive)
                .collect(Collectors.toList());
    }

    boolean is(String playerId) {
        return id.equals(playerId);
    }

    public static Player createOrGet(String id) {
        return PLAYERS.computeIfAbsent(id, Player::new);
    }

    public static PlayersListCommandDto requestListOfPlayers() {
        return PlayersListCommandDto.of(PLAYERS
                .values()
                .stream()
                .map(p -> PlayerDto.of(p.id, p.ship, p.x, p.y, p.angle))
                .collect(Collectors.toList()));
    }

    private void resetPosition() {
        if (Config.get().getEnv() == Config.Environment.DEV) {
            this.x = 0f;
            this.y = 0f;
        } else {
            this.x = RNG.nextFloat();
            this.y = RNG.nextFloat();
        }
        this.angle = 0f;
    }

    public PlayerJoinedEventDto join() {
        resetPosition();
        return PlayerJoinedEventDto.of(id, ship, x, y, angle);
    }

    public Optional<EventDto> move(float moveX, float moveY, Float angle) {

        if (Math.abs(moveX) <= MIN_MOVE &&
                Math.abs(moveY) <= MIN_MOVE
        ) {
            this.currentSpeed = 0f;
            if (angle == null) {
                return Optional.empty();
            }
        }

        float nx = this.x + moveX;
        float ny = this.y + moveY;
        if (angle != null) {
            this.angle = angle;
        }

        this.currentSpeed = (float) Math.sqrt(Math.pow(Math.abs(moveX), 2) + Math.pow(Math.abs(moveY), 2));


        this.x = Math.max(0f, Math.min(1f, nx));
        this.y = Math.max(0f, Math.min(1f, ny));

        return Optional.of(PlayerMovedEventDto.of(this.id, this.x, this.y, this.angle));
    }

    public PlayerLeftEventDto leave() {
        PLAYERS.remove(this.id);
        return PlayerLeftEventDto.of(this.id);
    }

    static Iterable<Player> iterable() {
        return PLAYERS.values();
    }

    boolean collision(float x1, float y1, float x2, float y2, float collisionRadius) {
        float minx = Math.min(x1, x2);
        float maxx = Math.max(x1, x2);
        if (this.x - collisionRadius > maxx || this.x + collisionRadius < minx) {
            return false;
        }

        float miny = Math.min(y1, y2);
        float maxy = Math.max(y1, y2);
        if (this.y - collisionRadius > maxy || this.y + collisionRadius < miny) {
            return false;
        }

        //https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
        float distance = Math.abs((y2 - y1) * this.x - (x2 - x1) * this.y + x2 * y1 - y2 * x1) / (float) Math.sqrt(Math.pow(y2 - y1, 2.) + Math.pow(x2 - x1, 2.));
        return distance <= collisionRadius;

    }

    List<EventDto> destroyedBy(String playerId) {
        resetPosition();

        List<EventDto> events = new ArrayList<>(2);
        events.add(PlayerDestroyedEventDto.of(this.id, playerId));
        events.add(PlayerMovedEventDto.of(this.id, this.x, this.y, this.angle));

        return events;
    }

    static void reset() {
        PLAYERS.clear();
    }
}
