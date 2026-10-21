package com.bayoumi.services.reminders;

import com.bayoumi.util.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ReminderUtil {
    private static ReminderUtil instance = null;
    private final List<Reminder> reminderList;
    private Date lastValidationDate;

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

    public void addAll(List<Reminder> reminders) {
        reminderList.addAll(reminders);
    }

    public void validate(Date date) {
        if (lastValidationDate == null) {
            lastValidationDate = new Date(date.getTime());
            return;
        }
        if (date.before(lastValidationDate)) {
            lastValidationDate = new Date(date.getTime());
            return;
        }

        final Iterator<Reminder> iterator = reminderList.iterator();
        while (iterator.hasNext()) {
            Reminder reminder = iterator.next();
            Date reminderDate = reminder.getDate();
            if (!reminderDate.before(lastValidationDate) && !reminderDate.after(date)) {
                iterator.remove();
                Logger.debug("reminder: " + reminder);
                reminder.getCallback().run();
            }
        }
        lastValidationDate = new Date(date.getTime());
    }

    public void clear() {
        reminderList.clear();
        lastValidationDate = null;
    }

    /**
     * Clears pending reminders and starts the next validation window at {@code date}.
     */
    public void clear(Date date) {
        reminderList.clear();
        lastValidationDate = new Date(date.getTime());
    }
}
