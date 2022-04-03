package me.pcasaes.hexoids.infrastructure.index;

import com.github.davidmoten.rtree2.Entries;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Point;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndex;
import me.pcasaes.hexoids.core.domain.model.Game;
import me.pcasaes.hexoids.core.domain.model.Player;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongPredicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@IfBuildProperty(name = "hexoids.config.infrastructure.players-spatial-index", stringValue = "rtree2")
@ApplicationScoped
public class RTree2PlayerSpatialIndex implements PlayerSpatialIndex {

    private static final Logger LOGGER = Logger.getLogger(RTree2PlayerSpatialIndex.class.getName());

    private final List<Player> results = new ArrayList<>();

    private final Vertx vertx;

    private final Updater updater;

    @Inject
    public RTree2PlayerSpatialIndex(GameQueue gameQueue, MeterRegistry meterRegistry, Vertx vertx) {
        this.vertx = vertx;
        this.updater = new Updater(gameQueue, meterRegistry);
        LOGGER.info("Using RTree2 Spatial Index");
    }

    public void startup(@Observes StartupEvent event) {
        this.vertx.deployVerticle(updater);
    }

    @PreDestroy
    public void stop() {
        this.vertx.undeploy(updater.deploymentID());
    }


    @Override
    public Iterable<Player> search(float x1, float y1, float x2, float y2, float distance) {
        RTree<Player, Point> index = this.updater.getIndex();
        if (index == null) {
            return Collections.emptyList();
        }
        this.results.clear();
        index
                .search(Geometries
                        .rectangle(
                                Math.min(x1, x2), Math.min(y1, y2),
                                Math.max(x1, x2), Math.max(y1, y2)
                        ), distance)
                .forEach(entry -> this.results.add(entry.value()));

        return this.results;
    }

    private static class Updater extends AbstractVerticle implements LongPredicate {

        private final GameQueue gameQueue;
        private final Timer timer;
        private boolean running = false;
        private RTree<Player, Point> index;

        private Updater(GameQueue gameQueue,
                        MeterRegistry meterRegistry) {
            this.gameQueue = gameQueue;
            this.timer = Timer.builder("player_spatial_index_update")
                    .description("Time to update the player spatial index.")
                    .publishPercentiles(0.5, 0.75, 0.90, 0.95)
                    .register(meterRegistry);
        }

        @Override
        public void start(Promise<Void> startPromise) throws Exception {
            this.context.runOnContext(h -> {
                running = true;
                this.gameQueue.enqueue(() -> Game.get().getPhysicsQueue().enqueue(this));
                startPromise.complete();
            });
        }

        @Override
        public void stop(Promise<Void> stopPromise) throws Exception {
            this.context
                    .runOnContext(h -> {
                        running = false;
                        stopPromise.complete();
                    });
        }

        @Override
        public boolean test(long l) {
            long now = System.nanoTime();
            final List<Player> list = new ArrayList<>();
            Game.get()
                    .getPlayers()
                    .forEach(list::add);

            update(list, now);

            return false;
        }

        private void update(List<Player> players, long start) {
            this.context
                    .runOnContext(h -> {
                        if (!running) {
                            return;
                        }

                        final RTree<Player, Point> rtree = RTree.create(
                                players
                                        .stream()
                                        .map(p -> Entries.entry(p, Geometries.point(p.getX(), p.getY())))
                                        .collect(Collectors.toList()) // NOSONAR: we want a mutable list
                        );

                        finishedUpdate(rtree, start);
                    });
        }

        private void finishedUpdate(RTree<Player, Point> result, long start) {
            this.gameQueue.enqueue(() -> {
                this.index = result;
                timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
                Game.get().getPhysicsQueue().enqueue(this);
            });
        }

        private RTree<Player, Point> getIndex() {
            return index;
        }
    }
}
