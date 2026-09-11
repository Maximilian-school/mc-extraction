package ca.maximilian.extraction_game.core.utils;

import lombok.Getter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Timer {

    @Getter
    private final int length;
    @Getter
    private int timeLeft;
    @Getter
    private Task countDownTask;
    @Getter
    private boolean isFinished;

    private final List<Runnable> finishListeners = new ArrayList<>();

    public Timer(int length) {
        this.length = length;
        this.timeLeft = length;
    }

    public Timer(Duration duration) {
        this((int) (duration.getSeconds() * 20));
    }

    public boolean isRunning() {
        return countDownTask != null && countDownTask.isAlive();
    }

    public void onFinish(Runnable listener) {
        finishListeners.add(listener);
    }

    public void reset() {
        if (countDownTask != null) {
            countDownTask.cancel();
        }

        countDownTask = null;
        timeLeft = length;
        isFinished = false;
    }

    public void start() {
        if (isRunning()) {
            return;
        }

        countDownTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    timeLeft--;

                    if (timeLeft <= 0) {
                        countDownTask.cancel();
                        countDownTask = null;
                        isFinished = true;

                        finishListeners.forEach(Runnable::run);
                    }
                })
                .repeat(TaskSchedule.tick(1))
                .schedule();
    }

    public void stop() {
        if (countDownTask != null) {
            countDownTask.cancel();
            countDownTask = null;
        }
    }
}