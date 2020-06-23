package me.pcasaes.hexoids.infrastructure.index;

import com.github.davidmoten.rtree2.Entries;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Point;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.scheduler.Scheduled;
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndex;
import me.pcasaes.hexoids.core.domain.model.Game;
import me.pcasaes.hexoids.core.domain.model.Player;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@IfBuildProperty(name = "hexoids.config.infrastructure.players-spatial-index", stringValue = "rtree2")
@ApplicationScoped
public class RTree2Player2SpatialIndex implements PlayerSpatialIndex {

    private static final Logger LOGGER = Logger.getLogger(RTree2Player2SpatialIndex.class.getName());

    private RTree<Player, Point> index;

    private final GameQueue gameQueue;

    private final List<Player> results = new ArrayList<>();

    private final AtomicBoolean taskRunning = new AtomicBoolean(false);


    @Inject
    public RTree2Player2SpatialIndex(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
        LOGGER.info("Using RTree2 Spatial Index");
    }


    @Scheduled(every = "1s")
    @Timed(name = "player-spatial-index-update", absolute = true, description = "Time to update the player spatial index.")
    public void task() {
        if (!taskRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            CountDownLatch latch = new CountDownLatch(1);
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
                    update(players.get());
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        } finally {
            this.taskRunning.set(false);
        }
    }

    private void update(List<Player> players) {

        final RTree<Player, Point> rtree = RTree.create(
                players
                        .stream()
                        .map(p -> Entries.entry(p, Geometries.point(p.getX(), p.getY())))
                        .collect(Collectors.toList())
        );

        this.gameQueue.enqueue(() -> this.index = rtree);
    }


    @Override
    public Iterable<Player> search(float x1, float y1, float x2, float y2, float distance) {
        if (this.index == null) {
            return Collections.emptyList();
        }
        this.results.clear();
        this.index
                .search(Geometries
                        .rectangle(
                                Math.min(x1, x2), Math.min(y1, y2),
                                Math.max(x1, x2), Math.max(y1, y2)
                        ), distance * 100f)
                .forEach(entry -> this.results.add(entry.value()));

        return this.results;
    }
}
