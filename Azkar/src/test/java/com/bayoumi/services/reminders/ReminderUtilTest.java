package com.bayoumi.services.reminders;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ReminderUtilTest {
    private ReminderUtil reminderUtil;

    @Before
    public void setUp() {
        reminderUtil = ReminderUtil.getInstance();
        reminderUtil.clear();
    }

    @After
    public void tearDown() {
        reminderUtil.clear();
    }

    @Test
    public void firesReminderOnceWhenValidationSkipsItsScheduledSecond() {
        final long baseline = 1_700_000_000_000L;
        final AtomicInteger callbackCount = new AtomicInteger();

        reminderUtil.validate(new Date(baseline));
        reminderUtil.add(new Reminder(new Date(baseline + 1_500), callbackCount::incrementAndGet));

        reminderUtil.validate(new Date(baseline + 900));
        reminderUtil.validate(new Date(baseline + 2_100));
        reminderUtil.validate(new Date(baseline + 3_100));

        assertEquals(1, callbackCount.get());
    }

    @Test
    public void doesNotRepeatReminderWhenSameValidationTimeIsSeenAgain() {
        final long baseline = 1_700_000_000_000L;
        final AtomicInteger callbackCount = new AtomicInteger();
        final Date dueTime = new Date(baseline + 1_000);

        reminderUtil.clear(new Date(baseline));
        reminderUtil.add(new Reminder(dueTime, callbackCount::incrementAndGet));
        reminderUtil.validate(dueTime);
        reminderUtil.validate(dueTime);

        assertEquals(1, callbackCount.get());
    }

    @Test
    public void doesNotFireReminderScheduledBeforeRebuildBaseline() {
        final long baseline = 1_700_000_000_000L;
        final AtomicInteger callbackCount = new AtomicInteger();

        reminderUtil.clear(new Date(baseline));
        reminderUtil.add(new Reminder(new Date(baseline - 1_000), callbackCount::incrementAndGet));
        reminderUtil.validate(new Date(baseline + 1_000));

        assertEquals(0, callbackCount.get());
    }
}
