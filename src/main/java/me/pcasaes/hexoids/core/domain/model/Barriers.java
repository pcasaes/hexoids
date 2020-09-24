package me.pcasaes.hexoids.core.domain.model;

import me.pcasaes.hexoids.core.domain.index.BarrierSpatialIndexFactory;
import me.pcasaes.hexoids.core.domain.utils.TrigUtil;
import me.pcasaes.hexoids.core.domain.vector.Vector2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Barriers implements Iterable<Barrier> {

    static Barriers create() {
        return new Barriers();
    }

    private final List<Barrier> barriers = new ArrayList<>();

    private boolean started = false;

    private Barriers() {
    }

    void fixedUpdate(long timestamp) {
        if (started) {
            return;
        }
        started = true;
        float offset = 2 * Barrier.LENGTH;
        for (int i = 0; i < 200; i++) {
            barriers.add(Barrier.place(Vector2.fromXY(i * Barrier.LENGTH + offset, 10 * offset), 0F));
        }
        for (int i = 0; i < 200; i++) {
            barriers.add(Barrier.place(Vector2.fromXY(10 * offset, i * Barrier.LENGTH + offset), TrigUtil.PI / 2f));
        }
        for (int i = 0; i < 200; i++) {
            barriers.add(Barrier.place(Vector2.fromXY(i * Barrier.LENGTH + offset, i * Barrier.LENGTH + offset), TrigUtil.PI / 4f));
        }

        if (BarrierSpatialIndexFactory.factory().get() == null) {
            BarrierSpatialIndexFactory
                    .factory()
                    .setBarrierSpatialIndex((x1, y1, x2, y2, d) -> this);
        } else {
            BarrierSpatialIndexFactory
                    .factory()
                    .get()
                    .update(this);
        }
    }

    @Override
    public Iterator<Barrier> iterator() {
        return barriers.iterator();
    }

    public Iterable<Barrier> search(float x1, float y1, float x2, float y2, float distance) {
        return BarrierSpatialIndexFactory
                .factory()
                .get()
                .search(x1, y1, x2, y2, distance);
    }
}
