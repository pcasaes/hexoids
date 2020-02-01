package me.pcasaes.hexoids.model;

import java.util.Random;

public interface ResetPosition {

    float getNextX();

    float getNextY();

    static ResetPosition create(String config) {
        if (Holder.instance == null) {
            if ("rng".equalsIgnoreCase(config)) {
                Holder.instance = new RngResetPosition();
            } else {
                Holder.instance = new FixedResetPosition(config);
            }
        }
        return Holder.instance;
    }

    class Holder {
        private Holder() {
        }

        private static ResetPosition instance;
    }

    class RngResetPosition implements ResetPosition {

        private static final Random RNG = new Random();

        private RngResetPosition() {
        }

        @Override
        public float getNextX() {
            return RNG.nextFloat();
        }

        @Override
        public float getNextY() {
            return RNG.nextFloat();
        }
    }

    class FixedResetPosition implements ResetPosition {

        private final float x;

        private final float y;

        private FixedResetPosition(String config) {
            if (config == null || config.length() == 0) {
                this.x = 0f;
                this.y = 0f;
            } else {
                String[] parts = config.split(",");
                this.x = Float.parseFloat(parts[0]);
                if (parts.length == 1) {
                    this.y = Float.parseFloat(parts[0]);
                } else {
                    this.y = Float.parseFloat(parts[1]);
                }
            }
        }

        @Override
        public float getNextX() {
            return x;
        }

        @Override
        public float getNextY() {
            return y;
        }
    }
}
