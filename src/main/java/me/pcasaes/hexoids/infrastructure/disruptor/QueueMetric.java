package me.pcasaes.hexoids.infrastructure.disruptor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class QueueMetric {

    public static final long LOAD_FACTOR_CALC_WINDOW_MILLIS = 1_000L;
    public static final long LOAD_FACTOR_CALC_WINDOW_MU = LOAD_FACTOR_CALC_WINDOW_MILLIS * 1000L;

    private static final List<QueueMetric> LIST = new CopyOnWriteArrayList<>();

    private long runningTime;
    private long lastReportTime;
    private long lastStartClock;
    private long lastCheckTime;

    private long nextAccumulate;

    private long latencyTotal;
    private long latencyCount;
    private double latency = 0.;

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
        this.runningTime += now - this.lastStartClock;
        this.lastCheckTime = System.currentTimeMillis();
    }

    void tallyLatency(long delayMu) {
        this.latencyTotal += delayMu;
        this.latencyCount++;
    }

    void accumulate() {
        long now = System.nanoTime();
        if (now >= this.nextAccumulate) {
            this.loadFactor = runningTime / (double) (now - this.lastReportTime);
            this.lastReportTime = now;
            this.runningTime = 0L;

            this.latency = this.latencyTotal / (double)  this.latencyCount;
            this.latencyTotal = 0L;
            this.latencyCount = 0L;

            this.nextAccumulate = now + LOAD_FACTOR_CALC_WINDOW_MU;
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

    public double getLatencyInMu() {
        return latency;
    }

    public long getLastCheckTimeAgo() {
        return System.currentTimeMillis() - lastCheckTime;
    }

    public String getName() {
        return name;
    }

    public static int compare(QueueMetric a, QueueMetric b) {
        return Double.compare(b.loadFactor, a.loadFactor);
    }


}
