import type { PrayerSnapshot } from '../panelState.js';

export const snapshot: PrayerSnapshot = {
    hidden: false,
    appName: 'Azkar',
    prayer: 'Fajr',
    time: '5:15 AM',
    remaining: '12 min',
    scheduleTitle: 'Prayer times',
    schedule: [{ key: 'fajr', name: 'Fajr', time: '5:15 AM', next: true }],
    open: 'Open Azkar',
    visibility: 'Hide panel',
    reminders: 'Pause reminders',
    remindersPaused: false,
    quit: 'Quit',
    close: 'Close',
    automatic: 'Automatic',
    manual: 'Manual',
    dragHint: 'Drag to move',
};
