package me.pcasaes.hexoids.entrypoints.jobs.periodictasks

import io.quarkus.runtime.StartupEvent
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Any
import jakarta.enterprise.inject.Instance
import jakarta.interceptor.Interceptor
import me.pcasaes.hexoids.core.domain.periodictasks.GamePeriodicTask
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.function.Consumer
import java.util.logging.Logger
import kotlin.concurrent.Volatile

@ApplicationScoped
class PeriodicTaskScheduler(
    @param:Any private val gamePeriodicTasksFactory: Instance<GamePeriodicTask>
) {
    private val gamePeriodicTasks = Collections.synchronizedList(
        ArrayList<GamePeriodicTask>()
    )

    @Volatile
    private lateinit var lowFreqScheduledExecutorService: ScheduledExecutorService

    @Volatile
    private lateinit var hiFreqScheduledExecutorService: ScheduledExecutorService

    fun startup(@Observes @Priority(Interceptor.Priority.APPLICATION + 900) event: StartupEvent?) {
        // eager load
    }

    @PostConstruct
    fun start() {
        gamePeriodicTasksFactory
            .forEach { gamePeriodicTasks.add(it) }

        this.lowFreqScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        this.hiFreqScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        gamePeriodicTasks
            .forEach(Consumer { task ->
                val log = StringBuilder("Starting periodic task ")
                    .append(task.javaClass.getSimpleName())
                    .append(": type=")
                val scheduledExecutorService: ScheduledExecutorService
                if (task.getTimeUnit().toMillis(task.getPeriod()) < 1000L) {
                    scheduledExecutorService = hiFreqScheduledExecutorService
                    log.append("high freq")
                } else {
                    scheduledExecutorService = lowFreqScheduledExecutorService
                    log.append("low freq")
                }

                log.append(", delay: ").append(task.getDelay())
                    .append(", period: ").append(task.getPeriod())
                    .append(", timeUnit: ").append(task.getTimeUnit())

                scheduledExecutorService.scheduleAtFixedRate(
                    task,
                    task.getDelay(),
                    task.getPeriod(),
                    task.getTimeUnit()
                )
                LOGGER.info(log.toString())
            })
    }

    @PreDestroy
    fun stop() {
        this.hiFreqScheduledExecutorService
            .shutdown()
        this.lowFreqScheduledExecutorService
            .shutdown()

        gamePeriodicTasks
            .forEach { instance -> gamePeriodicTasksFactory.destroy(instance) }
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(PeriodicTaskScheduler::class.java.getName())
    }
}
