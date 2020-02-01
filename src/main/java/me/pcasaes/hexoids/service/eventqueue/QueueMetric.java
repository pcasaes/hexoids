package me.pcasaes.hexoids.service.eventqueue;

import me.pcasaes.hexoids.model.Game;

import java.util.logging.Logger;

class QueueMetric {

    static final long LOAD_FACTOR_CALC_WINDOW_MILLIS = 3_000L;
    static final long LOAD_FACTOR_CALC_WINDOW_SECONDS = LOAD_FACTOR_CALC_WINDOW_MILLIS / 1000L;

    private static final Logger LOGGER = Logger.getLogger(QueueMetric.class.getName());

    private long runningTime;
    private long sleepingTime;
    private long lastTimestamp;
    private boolean isRunning = false;
    private double loadFactor = 0.;
    private long loadFactorTimestamp;
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    private long updateAndGetLastTimestamp() {
        long now = Game.get().getClock().getTime();
        long time = now - lastTimestamp;
        lastTimestamp = now;
        return time;
    }

    void running() {
        if (!isRunning) {
            sleepingTime += updateAndGetLastTimestamp();
            isRunning = true;
        }
    }

    void sleeping() {
        if (isRunning) {
            runningTime += updateAndGetLastTimestamp();
            isRunning = false;
        }
    }

    void calculateLoadFactor() {
        long now = Game.get().getClock().getTime();
        if (now - this.loadFactorTimestamp >= LOAD_FACTOR_CALC_WINDOW_MILLIS) {
            if (isRunning) {
                runningTime += updateAndGetLastTimestamp();
            } else {
                sleepingTime += updateAndGetLastTimestamp();
            }
            this.loadFactor = runningTime / (double) (runningTime + sleepingTime);
            this.runningTime = 0;
            this.sleepingTime = 0;
            this.loadFactorTimestamp = now;
        }
    }


    void report() {
        if (this.loadFactor < 0.5) {
            LOGGER.info(this::getMetricMessage);
        } else if (this.loadFactor < 0.8) {
            LOGGER.warning(this::getMetricMessage);
        } else {
            LOGGER.severe(this::getMetricMessage);
        }
    }

    private String getMetricMessage() {
        return "LOAD FACTOR " + this.name + ": " + this.loadFactor;
    }


}
