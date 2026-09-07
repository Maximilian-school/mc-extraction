package ca.maximilian.extraction_game.core;

import ca.maximilian.extraction_game.ExtractionGame;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Chunk;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

public class CoreLoop {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreLoop.class);

    private static final AtomicReference<Task> mainTickTask = new AtomicReference<>();
    private static final AtomicReference<Task> watchdogTask = new AtomicReference<>();

    private static volatile boolean shouldBeRunning = false;

    private static void tick() {

    }

    public static void start() {
        if (shouldBeRunning) return;
        shouldBeRunning = true;

        scheduleMainTick();

        Task watchdog = MinecraftServer.getSchedulerManager().buildTask(() -> {
                    if (!shouldBeRunning) return;

                    Task currentTickTask = mainTickTask.get();
                    if (currentTickTask == null || !currentTickTask.isAlive()) {
                        LOGGER.info("The core loop has crashed! Now restarting!");
                        scheduleMainTick();
                    }
                })
                .repeat(TaskSchedule.millis(500))
                .executionType(ExecutionType.TICK_END)
                .schedule();

        watchdogTask.set(watchdog);
    }

    private static void scheduleMainTick() {
        Task newTask = MinecraftServer.getSchedulerManager().buildTask(CoreLoop::tick)
                .repeat(TaskSchedule.tick(1))
                .schedule();
        mainTickTask.set(newTask);
    }

    public static void stop() {
        shouldBeRunning = false;

        Task tick = mainTickTask.getAndSet(null);
        if (tick != null && tick.isAlive()) {
            tick.cancel();
        }

        Task watchdog = watchdogTask.getAndSet(null);
        if (watchdog != null && watchdog.isAlive()) {
            watchdog.cancel();
        }
    }
}
