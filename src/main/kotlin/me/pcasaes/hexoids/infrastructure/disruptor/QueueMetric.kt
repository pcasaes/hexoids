package me.pcasaes.hexoids.infrastructure.disruptor;

import me.pcasaes.hexoids.infrastructure.clock.HRClock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class QueueMetric {

    public static final long NANO_PER_SECONDS = 1_000_000_000L;
    public static final long LOAD_FACTOR_CALC_WINDOW_MILLIS = 1_000L;
    public static final long LOAD_FACTOR_CALC_WINDOW_NANO = LOAD_FACTOR_CALC_WINDOW_MILLIS * 1_000_000L;
    public static final long STALLED_TIME_NANO = LOAD_FACTOR_CALC_WINDOW_NANO * 5L;

    private static final List<QueueMetric> LIST = new CopyOnWriteArrayList<>();

    private long runningTime;
    private long lastReportTime;
    private long lastStartClock;

    private long nextAccumulate;

    private long latencyTotal;
    private long eventCount;
    private volatile double latency = 0.;
    private volatile double avgProcessingTime = 0.;
    private volatile double loadFactor = 0.;
    private volatile double throughput = 0.;
    private volatile long lastAccumulationTime = 0;

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
        this.lastStartClock = HRClock.nanoTime();
    }

    void stopClock(long eventCreatedTime) {
        long now = HRClock.nanoTime();
        this.runningTime += now - this.lastStartClock;
        this.latencyTotal += now - eventCreatedTime;
        this.eventCount++;

        if (now >= this.nextAccumulate) {
            double count = this.eventCount;
            double elapsedTime = (double) (now - this.lastReportTime);
            this.throughput = count / (elapsedTime / NANO_PER_SECONDS);
            this.avgProcessingTime = runningTime / count;
            this.loadFactor = runningTime / elapsedTime;
            this.lastReportTime = now;
            this.runningTime = 0L;

            this.latency = this.latencyTotal / count;
            this.latencyTotal = 0L;
            this.eventCount = 0;

            this.nextAccumulate = now + LOAD_FACTOR_CALC_WINDOW_NANO;
            this.lastAccumulationTime = now;
        }
    }

    public boolean isOverCapacity() {
        long t = this.lastAccumulationTime;
        if (t == 0) {
            return false;
        }

        double l = this.loadFactor;
        return l >= 0.8;
    }

    public boolean isStalled() {
        long t = this.lastAccumulationTime;
        if (t == 0) {
            return false;
        }
        return HRClock.nanoTime() - t > STALLED_TIME_NANO;
    }

    public double getLoadFactor() {
        return loadFactor;
    }

    public double getLatencyInNano() {
        return latency;
    }

    public double getAvgProcessingTimeInNano() {
        return avgProcessingTime;
    }

    public double getThroughput() {
        return throughput;
    }

    public long getLastCheckTimeAgoNano() {
        return HRClock.nanoTime() - this.lastAccumulationTime;
    }

    public String getName() {
        return name;
    }

    public static int compare(QueueMetric a, QueueMetric b) {
        return Double.compare(b.loadFactor, a.loadFactor);
    }


}
