package ca.maximilian.extraction_game.core;

import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Timer {

    private final int length;
    private int timeLeft;
    private Task countDownTask;
    private boolean isFinished;

    private final List<Runnable> finishListeners = new ArrayList<>();

    public Timer(int length) {
        this.length = length;
        this.timeLeft = length;
    }

    public Timer(Duration duration) {
        this((int) (duration.getSeconds() * 20));
    }

    public int getLength() {
        return length;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public boolean isRunning() {
        return countDownTask != null && countDownTask.isAlive();
    }

    public Timer onFinish(Runnable listener) {
        finishListeners.add(listener);
        return this;
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