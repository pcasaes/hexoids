package me.pcasaes.bbop.service.eventqueue;

import me.pcasaes.bbop.model.Game;

import java.util.logging.Level;
import java.util.logging.Logger;

class QueueMetric {

    private static final Logger LOGGER = Logger.getLogger(QueueMetric.class.getName());

    private long runningTime;
    private long sleepingTime;
    private long lastTimestamp;
    private boolean isRunning = false;
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

    void report() {
        if (isRunning) {
            runningTime += updateAndGetLastTimestamp();
        } else {
            sleepingTime += updateAndGetLastTimestamp();
        }
        if (LOGGER.isLoggable(Level.INFO)) {
            double lf = runningTime / (double) (runningTime + sleepingTime);
            LOGGER.info("LOAD FACTOR " + this.name + ": " + lf);
        }
        runningTime = 0;
        sleepingTime = 0;
    }


}
