package me.pcasaes.hexoids.domain.model;

import java.util.Random;

/**
 * Whenever a player is spawned the class provides the player's X,Y position.
 *
 */
public interface ResetPosition {

    float getNextX();

    float getNextY();

    /**
     * Return a ResetPosition.
     *
     * If config is rng will return {@link RngResetPosition}
     * Otherwise presumes a comma separated float list and return {@link FixedResetPosition}
     *
     * @param config used to configure an appropriate implementation
     * @return a ResetPosition
     */
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

    /**
     * {@link ResetPosition} implementaiton that spawns players in a random
     * location.
     */
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

    /**
     * {@link ResetPosition} implementation that spawns all player in the
     * same X,Y position.
     */
    class FixedResetPosition implements ResetPosition {

        private final float x;

        private final float y;

        /**
         * The fixed position is defined in a comma separated list.
         * ex: 0,0
         *
         * Valid values are between 0 and 1.
         *
         * @param config comma separated X,Y position.
         */
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
