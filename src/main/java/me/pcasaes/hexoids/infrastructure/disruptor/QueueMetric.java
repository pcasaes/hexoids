package me.pcasaes.hexoids.infrastructure.disruptor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class QueueMetric {

    public static final long LOAD_FACTOR_CALC_WINDOW_MILLIS = 3_000L;
    public static final long LOAD_FACTOR_CALC_WINDOW_SECONDS = LOAD_FACTOR_CALC_WINDOW_MILLIS / 1000L;

    private static final Logger LOGGER = Logger.getLogger(QueueMetric.class.getName());
    private static final List<QueueMetric> LIST = new CopyOnWriteArrayList<>();

    private long runningTime;
    private long lastReportTime;
    private long lastStartClock;
    private long firstStartRunningTime;
    private long lastCheckTime;

    private double loadFactor = 0.;
    private final String name;

    private QueueMetric(String name) {
        this.name = name;
    }

    static QueueMetric of(String name) {
        QueueMetric metric = new QueueMetric(name);
        LIST.add(metric);
        return metric;
    }

    public static List<QueueMetric> getAllMetrics() {
        return Collections.unmodifiableList(LIST);
    }

    void startClock() {
        this.lastStartClock = System.nanoTime();
    }

    void stopClock() {
        long now = System.nanoTime();
        if (firstStartRunningTime < lastReportTime) {
            this.runningTime = 0L;
            firstStartRunningTime = now;
        }
        this.runningTime += now - this.lastStartClock;
        this.lastCheckTime = System.currentTimeMillis();
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

    public boolean isOverCapacity() {
        long t = this.lastCheckTime;
        if (t == 0) {
            return false;
        }

        double l = this.loadFactor;
        return l >= 0.8;
    }

    public boolean isStalled() {
        long t = this.lastCheckTime;
        if (t == 0) {
            return false;
        }
        return t + 5_000L < System.currentTimeMillis();
    }

    public double getLoadFactor() {
        return loadFactor;
    }

    public long getLastCheckTimeAgo() {
        return System.currentTimeMillis() - lastCheckTime;
    }

    public String getName() {
        return name;
    }

    private String getMetricMessage() {
        return "LOAD FACTOR " + this.name + ": " + this.loadFactor;
    }

    public static int compare(QueueMetric a, QueueMetric b) {
        return Double.compare(b.loadFactor, a.loadFactor);
    }


}
