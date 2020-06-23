package me.pcasaes.hexoids.infrastructure.index;

import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Point;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.scheduler.Scheduled;
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndex;
import me.pcasaes.hexoids.core.domain.model.Player;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@IfBuildProperty(name = "hexoids.config.infrastructure.players-spatial-index", stringValue = "rtree2")
@ApplicationScoped
public class RTree2PlayerSpatialIndex implements PlayerSpatialIndex {

    private static final Logger LOGGER = Logger.getLogger(RTree2PlayerSpatialIndex.class.getName());

    private RTree<Player, Point> index;

    private final List<Player> results = new ArrayList<>();

    private final AtomicBoolean taskRunning = new AtomicBoolean(false);

    private final RTree2PlayerSpatialIndexUpdater updater;

    @Inject
    public RTree2PlayerSpatialIndex(RTree2PlayerSpatialIndexUpdater updater) {
        this.updater = updater;
        LOGGER.info("Using RTree2 Spatial Index");
    }


    @Scheduled(every = "1s")
    public void task() {
        if (!taskRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            this.updater.update(r -> this.index = r);
        } finally {
            this.taskRunning.set(false);
        }
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
