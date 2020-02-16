package me.pcasaes.hexoids.service.eventqueue;

import java.util.logging.Logger;

class QueueMetric {

    static final long LOAD_FACTOR_CALC_WINDOW_MILLIS = 3_000L;
    static final long LOAD_FACTOR_CALC_WINDOW_SECONDS = LOAD_FACTOR_CALC_WINDOW_MILLIS / 1000L;

    private static final Logger LOGGER = Logger.getLogger(QueueMetric.class.getName());

    private long runningTime;
    private long lastReportTime;
    private long lastStartClock;
    private long firstStartRunningTime;

    private double loadFactor = 0.;
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public void startClock() {
        this.lastStartClock = System.nanoTime();
    }

    public void stopClock() {
        long now = System.nanoTime();
        if (firstStartRunningTime < lastReportTime) {
            this.runningTime = 0L;
            firstStartRunningTime = now;
        }
        this.runningTime += now - this.lastStartClock;
    }

    void report() {
        long now = System.nanoTime();
        this.loadFactor = runningTime / (double) (now - this.lastReportTime);
        this.lastReportTime = now;
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
