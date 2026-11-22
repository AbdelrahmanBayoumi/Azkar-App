import Clutter from 'gi://Clutter';
import Gio from 'gi://Gio';
import GLib from 'gi://GLib';
import St from 'gi://St';

import {Extension} from 'resource:///org/gnome/shell/extensions/extension.js';
import * as Main from 'resource:///org/gnome/shell/ui/main.js';
import * as PanelMenu from 'resource:///org/gnome/shell/ui/panelMenu.js';
import * as PopupMenu from 'resource:///org/gnome/shell/ui/popupMenu.js';

const APP_BUS = 'com.azkar.PrayerPanel';
const EXTENSION_BUS = 'com.azkar.PrayerPanelExtension';
const PANEL_PATH = '/com/azkar/PrayerPanel';
const PANEL_INTERFACE = 'com.azkar.PrayerPanel1';
const POSITIONS = ['left', 'center', 'right'];
const PRAYER_ICONS = {
    fajr: ['daytime-sunrise-symbolic', 'weather-clear-symbolic'],
    dhuhr: ['weather-clear-symbolic'],
    asr: ['weather-clear-symbolic'],
    maghrib: ['daytime-sunset-symbolic', 'weather-clear-symbolic'],
    isha: ['weather-clear-night-symbolic'],
};

export default class AzkarPrayerPanel extends Extension {
    enable() {
        this._position = this._readPosition();
        this._button = new PanelMenu.Button(0.5, this.metadata.name, true);
        this._summary = new St.BoxLayout({
            style_class: 'azkar-prayer-summary',
            y_align: Clutter.ActorAlign.CENTER,
        });
        const logo = new Gio.FileIcon({file: this.dir.get_child('azkar.png')});
        this._summary.add_child(new St.Icon({gicon: logo, style_class: 'system-status-icon'}));
        this._prayer = new St.Label({y_align: Clutter.ActorAlign.CENTER});
        this._countdown = new St.Label({
            style_class: 'azkar-prayer-countdown',
            y_align: Clutter.ActorAlign.CENTER,
        });
        this._summary.add_child(this._prayer);
        this._summary.add_child(this._countdown);
        this._button.add_child(this._summary);
        this._schedule = this._createMenu();
        this._actions = this._createMenu();
        this._preview = this._createMenu(false);
        this._button.track_hover = true;
        this._button.connect('notify::hover', () => this._updatePreview());
        this._button.connect('captured-event', (_actor, event) => this._handleEvent(event));
        Main.panel.addToStatusArea(this.uuid, this._button, 0, this._position ?? 'right');
        this._button.container.hide();
        this._connectApp();
    }

    disable() {
        this._endDrag();
        this._clearTooltip();
        this._cancellable?.cancel();
        this._cancellable = null;
        if (this._proxy) {
            for (const signal of this._proxySignals)
                this._proxy.disconnect(signal);
        }
        this._proxy = null;
        this._state = null;
        if (this._busOwner)
            Gio.bus_unown_name(this._busOwner);
        this._busOwner = 0;
        this._preview?.destroy();
        this._schedule?.destroy();
        this._actions?.destroy();
        this._button?.destroy();
        this._button = null;
    }

    _createMenu(managed = true) {
        const menu = new PopupMenu.PopupMenu(this._button, 0.5, St.Side.TOP);
        menu.actor.add_style_class_name('panel-menu');
        menu.actor.add_style_class_name('azkar-prayer-menu');
        Main.uiGroup.add_child(menu.actor);
        menu.actor.hide();
        if (managed)
            Main.panel.menuManager.addMenu(menu);
        else
            menu.actor.reactive = false;
        menu.connect('open-state-changed', (_, open) => {
            if (!open)
                this._clearTooltip();
        });
        return menu;
    }

    _connectApp() {
        const cancellable = this._cancellable = new Gio.Cancellable();
        Gio.DBusProxy.new(
            Gio.DBus.session, Gio.DBusProxyFlags.DO_NOT_AUTO_START, null,
            APP_BUS, PANEL_PATH, PANEL_INTERFACE, cancellable, (_obj, result) => {
                if (cancellable.is_cancelled())
                    return;
                try {
                    this._proxy = Gio.DBusProxy.new_finish(result);
                    this._proxySignals = [
                        this._proxy.connect('g-properties-changed', () => this._update()),
                        this._proxy.connect('notify::g-name-owner', () => this._update()),
                    ];
                    this._update();
                    this._busOwner = Gio.bus_own_name_on_connection(
                        Gio.DBus.session, EXTENSION_BUS, Gio.BusNameOwnerFlags.NONE, null, null);
                } catch (error) {
                    logError(error, 'Connecting to the Azkar prayer panel');
                }
            });
    }

    _update() {
        const value = this._proxy.get_name_owner()
            ? this._proxy.get_cached_property('Snapshot')?.unpack() : null;
        const previous = this._state;
        this._state = value ? JSON.parse(value) : null;
        const state = this._state;
        if (!state || previous?.hidden !== state.hidden) {
            this._moveArmed = false;
            this._endDrag();
            this._preview.close();
            this._schedule.close();
            this._actions.close();
        }
        this._button.container.visible = !!state;
        if (!state)
            return;
        const showPrayer = !state.hidden && !!state.prayer;
        this._prayer.visible = showPrayer;
        this._prayer.text = showPrayer ? `${state.prayer} · ${state.time}` : '';
        this._countdown.visible = showPrayer;
        this._countdown.text = showPrayer ? state.remaining : '';
        this._button.accessible_name = showPrayer
            ? `${state.prayer}, ${state.time}, ${state.remaining}` : state.appName;
    }

    _runAction(action) {
        this._proxy?.call('RunAction', new GLib.Variant('(s)', [action]),
            Gio.DBusCallFlags.NO_AUTO_START, 3000, this._cancellable, (proxy, result) => {
                try {
                    proxy.call_finish(result);
                } catch (error) {
                    if (this._proxy === proxy)
                        logError(error, 'Running an Azkar panel action');
                }
            });
    }

    _updatePreview() {
        const state = this._state;
        if (!this._button.hover || !state?.schedule.length || state.hidden ||
            this._drag || this._moveArmed || Main.panel.menuManager.activeMenu) {
            this._preview.close();
            return;
        }
        if (!this._preview.isOpen) {
            this._preview.removeAll();
            this._fillSchedule(this._preview, state, false);
            this._preview.open();
        }
    }

    _toggleMenu(actions) {
        this._preview.close();
        const state = this._state;
        if (!state)
            return;
        const menu = actions || state.hidden ? this._actions : this._schedule;
        if (menu.isOpen) {
            menu.close();
            return;
        }
        this._schedule.close();
        this._actions.close();
        menu.removeAll();
        if (menu === this._schedule)
            this._fillSchedule(menu, state);
        else
            this._fillActions(menu, state);
        menu.open();
    }

    _fillSchedule(menu, state, interactive = true) {
        menu.box.add_style_class_name('azkar-prayer-schedule');
        const header = new PopupMenu.PopupBaseMenuItem({reactive: false, can_focus: false});
        header.add_style_class_name('azkar-prayer-schedule-header');
        const title = new St.Label({
            text: state.scheduleTitle,
            style_class: 'azkar-prayer-heading',
            x_expand: true,
            y_align: Clutter.ActorAlign.CENTER,
        });
        header.add_child(new St.Icon({icon_name: 'x-office-calendar-symbolic', style_class: 'popup-menu-icon'}));
        header.add_child(title);
        if (interactive) {
            header.add_child(this._iconButton('go-home-symbolic', state.open, () => {
                menu.close();
                this._runAction('open');
            }));
            header.add_child(this._iconButton('window-close-symbolic', state.close, () => menu.close()));
        }
        menu.addMenuItem(header);

        for (const row of state.schedule) {
            const item = new PopupMenu.PopupBaseMenuItem({reactive: false, can_focus: false});
            item.add_style_class_name('azkar-prayer-row');
            if (row.next)
                item.add_style_class_name('azkar-prayer-next');
            item.add_child(new St.Icon({
                gicon: Gio.ThemedIcon.new_from_names(PRAYER_ICONS[row.key] ?? ['alarm-symbolic']),
                style_class: 'popup-menu-icon',
            }));
            item.add_child(new St.Label({text: row.name, x_expand: true}));
            item.add_child(new St.Label({text: row.time, style_class: 'azkar-prayer-time'}));
            menu.addMenuItem(item);
        }
    }

    _fillActions(menu, state) {
        menu.addAction(state.open, () => this._runAction('open'), 'go-home-symbolic');
        if (state.visibility) {
            menu.addAction(state.visibility, () => this._runAction('visibility'),
                state.hidden ? 'view-reveal-symbolic' : 'view-conceal-symbolic');
        }
        menu.addAction(state.reminders, () => this._runAction('reminders'),
            state.remindersPaused ? 'preferences-system-notifications-symbolic' : 'notifications-disabled-symbolic');
        menu.addMenuItem(new PopupMenu.PopupSeparatorMenuItem());
        const footer = new PopupMenu.PopupBaseMenuItem({reactive: false, can_focus: false});
        const quitLabel = new St.BoxLayout({style_class: 'azkar-prayer-quit-label'});
        quitLabel.add_child(new St.Icon({icon_name: 'window-close-symbolic', style_class: 'popup-menu-icon'}));
        quitLabel.add_child(new St.Label({text: state.quit}));
        const quit = new St.Button({
            child: quitLabel,
            accessible_name: state.quit,
            style_class: 'azkar-prayer-quit',
            x_expand: true,
            x_align: Clutter.ActorAlign.FILL,
            can_focus: true,
        });
        quit.connect('clicked', () => {
            menu.close();
            this._runAction('quit');
        });
        footer.add_child(quit);
        if (!state.hidden)
            this._addPositionControls(footer, menu, state);
        menu.addMenuItem(footer);
    }

    _addPositionControls(row, menu, state) {
        const automatic = this._iconButton('emblem-default-symbolic', state.automatic, () => {
            menu.close();
            this._moveArmed = false;
            this._place(null);
        });
        const manual = this._iconButton('list-drag-handle-symbolic', `${state.manual}. ${state.dragHint}`, () => {
            menu.close();
            this._moveArmed = true;
            this._button.grab_key_focus();
        });
        (this._position ? manual : automatic).add_style_class_name('azkar-prayer-position-selected');
        row.add_child(automatic);
        row.add_child(manual);
    }

    _iconButton(icon, label, callback) {
        const button = new St.Button({
            child: new St.Icon({icon_name: icon, style_class: 'popup-menu-icon'}),
            style_class: 'azkar-prayer-icon-button',
            accessible_name: label,
            track_hover: true,
            can_focus: true,
        });
        button.connect('clicked', callback);
        button.connect('notify::hover', () => {
            if (button.hover)
                this._showTooltip(button, label);
            else
                this._clearTooltip();
        });
        return button;
    }

    _showTooltip(anchor, text) {
        this._clearTooltip();
        const tooltip = this._tooltip = new St.Label({text, style_class: 'azkar-prayer-tooltip'});
        Main.uiGroup.add_child(tooltip);
        const [x, y] = anchor.get_transformed_position();
        const [anchorWidth, anchorHeight] = anchor.get_transformed_size();
        const [, width] = tooltip.get_preferred_width(-1);
        const [, height] = tooltip.get_preferred_height(width);
        const monitor = Main.layoutManager.primaryMonitor;
        const left = Math.max(monitor.x, Math.min(x + (anchorWidth - width) / 2, monitor.x + monitor.width - width));
        tooltip.set_position(left, y - height - 4 >= monitor.y ? y - height - 4 : y + anchorHeight + 4);
    }

    _clearTooltip() {
        this._tooltip?.destroy();
        this._tooltip = null;
    }

    _handleEvent(event) {
        if (this._drag)
            return this._dragEvent(event);
        const type = event.type();
        if (type === Clutter.EventType.BUTTON_PRESS) {
            if ((this._moveArmed || this._position) && !this._state?.hidden &&
                event.get_button() === Clutter.BUTTON_PRIMARY)
                this._beginDrag(event.get_coords());
            else {
                this._moveArmed = false;
                this._toggleMenu(event.get_button() === Clutter.BUTTON_SECONDARY);
            }
            return Clutter.EVENT_STOP;
        }
        if (type === Clutter.EventType.TOUCH_BEGIN) {
            this._toggleMenu(false);
            return Clutter.EVENT_STOP;
        }
        if (type === Clutter.EventType.KEY_PRESS)
            return this._keyPress(event);
        return Clutter.EVENT_PROPAGATE;
    }

    _keyPress(event) {
        const key = event.get_key_symbol();
        if (this._moveArmed && (key === Clutter.KEY_Left || key === Clutter.KEY_Right)) {
            const index = POSITIONS.indexOf(this._position ?? 'right');
            this._place(POSITIONS[Math.max(0, Math.min(2, index + (key === Clutter.KEY_Left ? -1 : 1)))]);
            this._button.grab_key_focus();
        } else if (key === Clutter.KEY_Escape && this._moveArmed) {
            this._moveArmed = false;
        } else if (key === Clutter.KEY_Menu || (key === Clutter.KEY_F10 && (event.get_state() & Clutter.ModifierType.SHIFT_MASK))) {
            this._toggleMenu(true);
        } else if (key === Clutter.KEY_Return || key === Clutter.KEY_space) {
            this._toggleMenu(false);
        } else {
            return Clutter.EVENT_PROPAGATE;
        }
        return Clutter.EVENT_STOP;
    }

    _beginDrag([pressX]) {
        this._preview.close();
        const [x, y] = this._summary.get_transformed_position();
        const [width, height] = this._summary.get_transformed_size();
        const preview = new Clutter.Clone({source: this._summary, width, height, opacity: 220, reactive: false});
        const grab = global.stage.grab(this._button);
        Main.uiGroup.add_child(preview);
        Main.uiGroup.set_child_above_sibling(preview, null);
        preview.set_position(x, y);
        this._button.opacity = 0;
        this._drag = {preview, grab, pressX, offset: pressX - x, y};
    }

    _dragEvent(event) {
        const type = event.type();
        if (type === Clutter.EventType.MOTION) {
            const [x] = event.get_coords();
            this._drag.preview.set_position(x - this._drag.offset, this._drag.y);
        } else if (type === Clutter.EventType.BUTTON_RELEASE && event.get_button() === Clutter.BUTTON_PRIMARY) {
            const [x] = event.get_coords();
            const moved = Math.abs(x - this._drag.pressX) > 4;
            const center = x - this._drag.offset + this._drag.preview.width / 2;
            this._endDrag();
            if (moved) {
                const monitor = Main.layoutManager.primaryMonitor;
                const section = Math.floor(3 * (center - monitor.x) / monitor.width);
                this._place(POSITIONS[Math.max(0, Math.min(2, section))]);
            } else {
                this._toggleMenu(false);
            }
        } else if (type === Clutter.EventType.KEY_PRESS && event.get_key_symbol() === Clutter.KEY_Escape) {
            this._endDrag();
        }
        return Clutter.EVENT_STOP;
    }

    _endDrag() {
        if (!this._drag)
            return;
        const {grab, preview} = this._drag;
        this._drag = null;
        this._moveArmed = false;
        preview.destroy();
        this._button.opacity = 255;
        grab.dismiss();
    }

    _place(position) {
        const target = Main.panel[`_${position ?? 'right'}Box`];
        if (!target)
            return;
        const container = this._button.container;
        if (container.get_parent() !== target) {
            container.get_parent().remove_child(container);
            target.insert_child_at_index(container, 0);
        }
        this._position = position;
        const file = this._positionFile();
        GLib.mkdir_with_parents(file.get_parent().get_path(), 0o700);
        try {
            GLib.file_set_contents(file.get_path(), position ?? 'automatic');
        } catch (error) {
            logError(error, 'Saving the Azkar panel position');
        }
    }

    _positionFile() {
        return Gio.File.new_for_path(GLib.build_filenamev([GLib.get_user_config_dir(), 'azkar', 'gnome-panel-position']));
    }

    _readPosition() {
        const file = this._positionFile();
        try {
            const [, bytes] = file.load_contents(null);
            const position = new TextDecoder().decode(bytes).trim();
            return POSITIONS.includes(position) ? position : null;
        } catch (error) {
            if (!error.matches(Gio.IOErrorEnum, Gio.IOErrorEnum.NOT_FOUND))
                logError(error, 'Reading the Azkar panel position');
            return null;
        }
    }
}
