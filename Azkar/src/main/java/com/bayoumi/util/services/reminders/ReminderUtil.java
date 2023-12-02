package com.bayoumi.util.services.reminders;

import com.bayoumi.util.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReminderUtil {
    private static ReminderUtil instance = null;
    private final List<Reminder> reminderList;

    private ReminderUtil() {
        reminderList = new ArrayList<>();
    }


    public static ReminderUtil getInstance() {
        if (instance == null) {
            instance = new ReminderUtil();
        }
        return instance;
    }

    public void add(Reminder reminder) {
        reminderList.add(reminder);
    }

    public void validate(Date date) {
        reminderList.forEach(reminder -> {
            if (isEqualIgnoreMillis(date, reminder.getDate())) {
                Logger.debug("reminder: " + reminder);
                reminder.getCallback().run();
            }
        });
    }

    public void clear() {
        reminderList.clear();
    }

    private boolean isEqualIgnoreMillis(Date a, Date b) {
        return ((a.getTime() / 1000) * 1000) == ((b.getTime() / 1000) * 1000);
    }
}
