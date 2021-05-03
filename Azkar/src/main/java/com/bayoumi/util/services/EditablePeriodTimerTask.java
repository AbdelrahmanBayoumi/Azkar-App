package com.bayoumi.util.services;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

/**
 * {@link TimerTask} with modifiable execution period.
 *
 * @author Bayoumi
 */
public class EditablePeriodTimerTask extends TimerTask {

    private final Runnable task;
    private final Supplier<Long> period;
    private Timer timer;

    /**
     * Constructor with task and supplier for period
     *
     * @param task   the task to execute in {@link TimerTask#run()}
     * @param period a provider for the period between task executions
     */
    public EditablePeriodTimerTask(Runnable task, Supplier<Long> period) {
        super();
        Objects.requireNonNull(task);
        Objects.requireNonNull(period);
        this.task = task;
        this.period = period;
        timer = new Timer();
    }

    public final void updateTimer() {
        Long p = period.get();
        Objects.requireNonNull(p);
//        System.out.println("updateTimer():- " + Thread.currentThread().getName());
//        System.out.println("updateTimer():- " + "new : " + p);
        System.out.println(String.format("Period set to: %d s", p / 1000));
        stopTask();
        timer = new Timer();
        timer.schedule(new EditablePeriodTimerTask(task, period), p, p);
    }

    public void stopTask() {
        if (timer != null) {
//            this.cancel();
            timer.cancel();
            timer.purge();
        }
        timer = null;
    }

    @Override
    public void run() {
        task.run();
        System.out.println("run():- " + Thread.currentThread().getName());
//        updateTimer("run()");
    }

}