package me.pcasaes.hexoids.entrypoints.jobs.periodictasks;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import me.pcasaes.hexoids.core.domain.periodictasks.GamePeriodicTask;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Flush;

import java.util.function.Consumer;

@ApplicationScoped
@IfBuildProperty(
        name = "hexoids.config.service.client-broadcast.enabled",
        stringValue = "true",
        enableIfMissing = true)
@Named("FlushClientBroadcasterPeriodicTask")
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
