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

        createCenterDiamond();
        createCornerSquares();


        BarrierSpatialIndexFactory
                .factory()
                .get()
                .update(this);

    }

    private void createCornerSquares() {

        int iter = (int) (0.25F / Barrier.LENGTH) - 6;

        Barrier s = Barrier.place(Vector2.fromXY(0.25F, 0F), TrigUtil.PI / 2f);
        for (int i = 0; i < iter; i++) {
            s = s.extend();
            if (i > 6) {
                barriers.add(s);
            }
        }

        s = Barrier.place(Vector2.fromXY(0F, 0.25F), 0);
        for (int i = 0; i < iter; i++) {
            s = s.extend();
            if (i > 6) {
                barriers.add(s);
            }
        }

        s = Barrier.place(Vector2.fromXY(0.25F, 0.75F), TrigUtil.PI / 2f);
        for (int i = 0; i < iter; i++) {
            s = s.extend();
            if (i > 6) {
                barriers.add(s);
            }
        }

        s = Barrier.place(Vector2.fromXY(0F, 0.75F), 0);
        for (int i = 0; i < iter; i++) {
            s = s.extend();
            if (i > 6) {
                barriers.add(s);
            }
        }


        s = Barrier.place(Vector2.fromXY(0.75F, 0F), TrigUtil.PI / 2f);
        for (int i = 0; i < iter; i++) {
            s = s.extend();
            if (i > 6) {
                barriers.add(s);
            }
        }

        s = Barrier.place(Vector2.fromXY(0.75F, 0.25F), 0);
        for (int i = 0; i < iter; i++) {
            s = s.extend();
            if (i > 6) {
                barriers.add(s);
            }
        }

        s = Barrier.place(Vector2.fromXY(0.75F, 0.75F), TrigUtil.PI / 2f);
        for (int i = 0; i < iter; i++) {
            s = s.extend();
            if (i > 6) {
                barriers.add(s);
            }
        }

        s = Barrier.place(Vector2.fromXY(0.75F, 0.75F), 0);
        for (int i = 0; i < iter; i++) {
            s = s.extend();
            if (i > 6) {
                barriers.add(s);
            }
        }
    }

    private void createCenterDiamond() {
        int iter = (int) (Math.sqrt(Math.pow(0.5F - 0.25F, 2F) + Math.pow(0.75F - 0.5F, 2F)) / Barrier.LENGTH) - 6;

        Barrier s = Barrier.place(Vector2.fromXY(0.25F, 0.5F), TrigUtil.PI / 4f);
        for (int i = 0; i < iter; i++) {
            s = s.extend();
            if (i > 6) {
                barriers.add(s);
            }
        }

        s = Barrier.place(Vector2.fromXY(0.25F, 0.5F), TrigUtil.PI / -4f);
        for (int i = 0; i < iter; i++) {
            s = s.extend();
            if (i > 6) {
                barriers.add(s);
            }
        }

        s = Barrier.place(Vector2.fromXY(0.75F, 0.5F), 3 * TrigUtil.PI / 4f);
        for (int i = 0; i < iter; i++) {
            s = s.extend();
            if (i > 6) {
                barriers.add(s);
            }
        }

        s = Barrier.place(Vector2.fromXY(0.75F, 0.5F), 3 * TrigUtil.PI / -4f);
        for (int i = 0; i < iter; i++) {
            s = s.extend();
            if (i > 6) {
                barriers.add(s);
            }
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
