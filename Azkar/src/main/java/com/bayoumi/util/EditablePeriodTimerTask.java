package com.bayoumi.util;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

/**
 * {@link TimerTask} with modifiable execution period.
 *
 * @author Datz
 */
public class EditablePeriodTimerTask extends TimerTask {

    private Runnable task;
    private Supplier<Long> period;
    private Long oldP;

    /**
     * Constructor with task and supplier for period
     *
     * @param task the task to execute in {@link TimerTask#run()}
     * @param period a provider for the period between task executions
     */
    public EditablePeriodTimerTask(Runnable task, Supplier<Long> period) {
        super();
        Objects.requireNonNull(task);
        Objects.requireNonNull(period);
        this.task = task;
        this.period = period;
    }

    private EditablePeriodTimerTask(Runnable task, Supplier<Long> period, Long oldP) {
        this(task, period);
        this.oldP = oldP;
    }

    public final void updateTimer() {
        Long p = period.get();
        Objects.requireNonNull(p);
        if (oldP == null || !oldP.equals(p)) {
            System.out.println(String.format("Period set to: %d s", p / 1000));
            cancel();
            new Timer().schedule(new EditablePeriodTimerTask(task, period, p), p, p);
            // new Timer().scheduleAtFixedRate(new EditablePeriodTimerTask(task, period), p, p);
        }
    }

    @Override
    public void run() {
        task.run();
        updateTimer();
    }

}