package me.pcasaes.hexoids.infrastructure.index;

import com.github.davidmoten.rtree2.Entries;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Rectangle;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import me.pcasaes.hexoids.core.domain.index.BarrierSpatialIndex;
import me.pcasaes.hexoids.core.domain.model.Barrier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class RTree2BarrierSpatialIndex implements BarrierSpatialIndex {

    private RTree<Barrier, Rectangle> index;

    private final List<Barrier> results = new ArrayList<>();

    public void startup(@Observes StartupEvent event) {
        // eager startup
    }

    @Override
    public Iterable<Barrier> search(float x1, float y1, float x2, float y2, float distance) {
        if (this.index == null) {
            return Collections.emptyList();
        }
        this.results.clear();
        this.index
                .search(Geometries
                        .rectangle(
                                Math.min(x1, x2), Math.min(y1, y2),
                                Math.max(x1, x2), Math.max(y1, y2)
                        ), distance)
                .forEach(entry -> this.results.add(entry.value()));

        return this.results;
    }

    @Override
    public void update(Iterable<Barrier> barriers) {
        index = RTree.create(
                StreamSupport.stream(barriers.spliterator(), false)
                        .map(p -> Entries.entry(p, Geometries
                                .rectangle(
                                        Math.min(p.getFrom().getX(), p.getTo().getX()), Math.min(p.getFrom().getY(), p.getTo().getY()),
                                        Math.max(p.getFrom().getX(), p.getTo().getX()), Math.max(p.getFrom().getY(), p.getTo().getY())
                                ))
                        )
                        .collect(Collectors.toList())
        );
    }
}
