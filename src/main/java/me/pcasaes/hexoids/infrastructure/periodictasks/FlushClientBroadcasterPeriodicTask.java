package me.pcasaes.hexoids.infrastructure.periodictasks;

import me.pcasaes.hexoids.domain.periodictasks.GamePeriodicTask;
import me.pcasaes.hexoids.infrastructure.broadcaster.ClientBroadcaster;
import me.pcasaes.hexoids.infrastructure.disruptor.DisruptorIn;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Flush;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class FlushClientBroadcasterPeriodicTask implements GamePeriodicTask {

    private static final Dto FLUSH = Dto
            .newBuilder()
            .setFlush(Flush.newBuilder())
            .build();

    private final DisruptorIn disruptorIn;
    private final ClientBroadcaster clientBroadcaster;

    @Inject
    public FlushClientBroadcasterPeriodicTask(DisruptorIn disruptorIn,
                                              ClientBroadcaster clientBroadcaster) {
        this.disruptorIn = disruptorIn;
        this.clientBroadcaster = clientBroadcaster;
    }

    private void clientFlushPeriodicTask() {
        if (clientBroadcaster.canFlush()) {
            flush();
        }
    }


    private void flush() {
        disruptorIn.enqueueClient(FLUSH);
    }

    @Override
    public long getDelay() {
        return 1000;
    }

    @Override
    public long getPeriod() {
        return this.clientBroadcaster.getBatchTimeout();
    }

    @Override
    public void run() {
        this.clientFlushPeriodicTask();
    }
}
