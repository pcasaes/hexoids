package me.pcasaes.hexoids.entrypoints.jobs.periodictasks;

import io.quarkus.arc.properties.IfBuildProperty;
import me.pcasaes.hexoids.core.domain.periodictasks.GamePeriodicTask;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Flush;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.function.Consumer;

@ApplicationScoped
@IfBuildProperty(
        name = "hexoids.config.service.client-broadcast.enabled",
        stringValue = "true",
        enableIfMissing = true)
public class FlushClientBroadcasterPeriodicTask implements GamePeriodicTask {

    private static final Dto FLUSH = Dto
            .newBuilder()
            .setFlush(Flush.newBuilder())
            .build();

    private final ClientQueue clientQueue;
    private final int batchTimeout;

    @Inject
    public FlushClientBroadcasterPeriodicTask(ClientQueue clientQueue,
                                              @ConfigProperty(
                                                      name = "hexoids.config.service.client-broadcast.batch.timeout",
                                                      defaultValue = "20"
                                              ) int batchTimeout) {
        this.clientQueue = clientQueue;
        this.batchTimeout = batchTimeout;
    }


    private void flush() {
        clientQueue.accept(FLUSH);
    }

    @Override
    public long getDelay() {
        return 1000;
    }

    @Override
    public long getPeriod() {
        return this.batchTimeout;
    }

    @Override
    public void run() {
        this.flush();
    }

    public interface ClientQueue extends Consumer<Dto> {
    }
}
