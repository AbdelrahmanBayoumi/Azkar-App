import Gio from 'gi://Gio';
import GLib from 'gi://GLib';
import GdkPixbuf from 'gi://GdkPixbuf';
import Shell from 'gi://Shell';

import PrayerPanel from './panel.js';

const output = GLib.getenv('AZKAR_CAPTURE_OUTPUT');
const snapshot = {
    palette: {
        surface: '#121b2c',
        text: '#e2e8f4',
        border: '#23314a',
        highlight: '#1a2940',
        accent: '#9ac9ff',
    },
    hidden: false,
    appName: 'Azkar',
    prayer: 'Dhuhr',
    time: '11:30 AM',
    remaining: '2h 33m',
    scheduleTitle: "Today's Prayer Times",
    schedule: [
        ['Fajr', '4:51 AM'],
        ['Dhuhr', '11:30 AM'],
        ['Asr', '2:19 PM'],
        ['Maghrib', '4:39 PM'],
        ['Isha', '5:59 PM'],
    ].map(([name, time]) => ({ key: name.toLowerCase(), name, time, next: name === 'Dhuhr' })),
    open: 'Open Azkar',
    visibility: 'Hide panel',
    reminders: 'Pause reminders',
    remindersPaused: false,
    quit: 'Quit',
    close: 'Close',
    automatic: 'Automatic',
    manual: 'Manual',
    dragHint: 'Drag the widget to move it.',
};

function settle() {
    return new Promise(resolve => {
        GLib.timeout_add(GLib.PRIORITY_DEFAULT, 500, () => {
            resolve();
            return GLib.SOURCE_REMOVE;
        });
    });
}

async function capture(name, menu) {
    const screenshot = new Shell.Screenshot();
    const path = `${output}/stage.png`;
    const stream = Gio.File.new_for_path(path).replace(null, false, Gio.FileCreateFlags.NONE, null);
    try {
        await new Promise((resolve, reject) => {
            screenshot.screenshot(false, stream, (_, result) => {
                try {
                    screenshot.screenshot_finish(result);
                    resolve();
                } catch (error) {
                    reject(error);
                }
            });
        });
    } finally {
        stream.close(null);
    }
    const stage = GdkPixbuf.Pixbuf.new_from_file(path);
    const [x, y] = menu.box.get_transformed_position();
    const [width, height] = menu.box.get_transformed_size();
    const left = Math.max(0, Math.floor(x) - 14);
    const right = Math.min(stage.width, Math.ceil(x + width) + 14);
    const bottom = Math.min(stage.height, Math.ceil(y + height) + 14);
    stage.new_subpixbuf(left, 0, right - left, bottom).savev(`${output}/gnome-prayer-${name}.png`, 'png', [], []);
}

export default class PrayerPanelCapture extends PrayerPanel {
    // Override the app connection in the temporary copy used for capture.
    _connectApp() {
        const property = new GLib.Variant('s', JSON.stringify(snapshot));
        this._proxy = {
            get_name_owner: () => 'preview',
            get_cached_property: () => property,
            call: () => {},
        };
        this._update();
        this._captureMenus().catch(error => GLib.file_set_contents(`${output}/error`, String(error)));
    }

    async _captureMenus() {
        await settle();
        this._toggleMenu(false, 'mouse');
        await settle();
        await capture('schedule', this._schedule);
        this._toggleMenu(true, 'mouse');
        await settle();
        await capture('actions', this._actions);
        GLib.file_set_contents(`${output}/done`, '');
    }
}
