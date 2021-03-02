package me.pcasaes.hexoids.infrastructure.index;

import com.github.davidmoten.rtree2.Entries;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Point;
import io.micrometer.core.annotation.Timed;
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.model.Game;
import me.pcasaes.hexoids.core.domain.model.Player;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Dependent
class RTree2PlayerSpatialIndexUpdater {

    private final GameQueue gameQueue;

    @Inject
    public RTree2PlayerSpatialIndexUpdater(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
    }

    @Timed(value = "player-spatial-index-update",
            description = "Time to update the player spatial index.",
            percentiles = {0.5, 0.75, 0.90, 0.95}
    )
    void update(Consumer<RTree<Player, Point>> consumer) {
        final CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<Player>> players = new AtomicReference<>();
        gameQueue.enqueue(() -> {
            List<Player> list = new ArrayList<>();
            Game.get().getPlayers()
                    .forEach(list::add);
            players.set(list);
            latch.countDown();
        });

        try {
            if (latch.await(15, TimeUnit.SECONDS)) {
                update(players.get(), consumer);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void update(List<Player> players, Consumer<RTree<Player, Point>> consumer) {

        final RTree<Player, Point> rtree = RTree.create(
                players
                        .stream()
                        .map(p -> Entries.entry(p, Geometries.point(p.getX(), p.getY())))
                        .collect(Collectors.toList())
        );

        this.gameQueue.enqueue(() -> consumer.accept(rtree));
    }
}
