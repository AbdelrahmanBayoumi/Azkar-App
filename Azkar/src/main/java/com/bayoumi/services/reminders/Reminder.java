package com.bayoumi.services.reminders;

import java.util.Date;

public class Reminder {
    private Date date;
    private Runnable callback;

    public Reminder(Date date, Runnable callback) {
        this.date = date;
        this.callback = callback;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Runnable getCallback() {
        return callback;
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

    @Override
    public String toString() {
        return "Reminder " + hashCode() + " {" + "date=" + date + '}';
    }
}
