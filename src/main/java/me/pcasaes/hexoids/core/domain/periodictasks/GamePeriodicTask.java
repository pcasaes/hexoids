package me.pcasaes.hexoids.core.domain.periodictasks;

import java.util.concurrent.TimeUnit;

public interface GamePeriodicTask extends Runnable {

    long getPeriod();

    default long getDelay() {
        return 1000;
    }

    default TimeUnit getTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }


}
