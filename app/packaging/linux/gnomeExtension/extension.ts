import Clutter from 'gi://Clutter';
import Gio from 'gi://Gio';
import GLib from 'gi://GLib';
import St from 'gi://St';

import { Extension } from 'resource:///org/gnome/shell/extensions/extension.js';
import * as Main from 'resource:///org/gnome/shell/ui/main.js';
import * as PanelMenu from 'resource:///org/gnome/shell/ui/panelMenu.js';
import * as PopupMenu from 'resource:///org/gnome/shell/ui/popupMenu.js';
import {
    POSITIONS,
    type Position,
    type PrayerSnapshot,
    parsePosition,
    parsePrayerSnapshot,
    positionAt,
} from './panelState.js';

const APP_BUS = 'com.azkar.PrayerPanel';
const EXTENSION_BUS = 'com.azkar.PrayerPanelExtension.Ready1';
const PANEL_PATH = '/com/azkar/PrayerPanel';
const PANEL_INTERFACE = 'com.azkar.PrayerPanel1';
type Action = 'open' | 'visibility' | 'reminders' | 'quit';

const DRAG_THRESHOLD = 4;

interface PanelDrag {
    preview: Clutter.Clone;
    grab: Clutter.Grab;
    moved: boolean;
    pressX: number;
    offset: number;
    y: number;
}

const PRAYER_ICONS: Record<string, string> = {
    fajr: 'sunrise',
    dhuhr: 'sun',
    asr: 'sun',
    maghrib: 'sunset',
    isha: 'moon',
};

const LOG_PREFIX = '[Azkar Prayer Panel]';

export default class AzkarPrayerPanel extends Extension {
    private _position: Position | null = null;
    private _button!: PanelMenu.Button;
    private _summary!: St.BoxLayout;
    private _prayer!: St.Label;
    private _countdown!: St.Label;
    private _schedule!: PopupMenu.PopupMenu;
    private _actions!: PopupMenu.PopupMenu;
    private _proxy: Gio.DBusProxy | null = null;
    private _proxySignals: number[] = [];
    private _cancellable: Gio.Cancellable | null = null;
    private _state: PrayerSnapshot | null = null;
    private _busOwner = 0;
    private _connectionStatus: string | null = null;
    private _moveArmed = false;
    private _drag: PanelDrag | null = null;
    private _tooltip: St.Label | null = null;

    private _log(message: string) {
        console.log(`${LOG_PREFIX} ${message}`);
        this._forwardLog('info', message);
    }

    private _reportError(error: unknown, message: string) {
        logError(error instanceof Object ? error : new Error(String(error)), `${LOG_PREFIX} ${message}`);
        this._forwardLog('error', `${message}: ${String(error)}`);
    }

    private _forwardLog(level: 'info' | 'error', message: string) {
        if (!this._busOwner || !this._proxy?.get_name_owner()) return;
        this._proxy.call(
            'LogEvent',
            new GLib.Variant('(ss)', [level, message]),
            Gio.DBusCallFlags.NO_AUTO_START,
            3000,
            null,
            null,
        );
    }

    enable() {
        this._position = this._readPosition();
        this._log(`Enabled with click-only schedule and ${this._position ?? 'automatic'} placement`);
        this._button = new PanelMenu.Button(0.5, this.metadata.name, true);
        this._summary = new St.BoxLayout({
            style_class: 'azkar-prayer-summary',
            y_align: Clutter.ActorAlign.CENTER,
        });
        const logo = new Gio.FileIcon({ file: this.dir.get_child('azkar.png') });
        // St and Gio typings resolve different copies of Gio.Icon.
        this._summary.add_child(
            new St.Icon({
                gicon: logo as unknown as NonNullable<St.Icon.ConstructorProps['gicon']>,
                style_class: 'system-status-icon',
            }),
        );
        this._prayer = new St.Label({ y_align: Clutter.ActorAlign.CENTER });
        this._countdown = new St.Label({
            style_class: 'azkar-prayer-countdown',
            y_align: Clutter.ActorAlign.CENTER,
        });
        this._summary.add_child(this._prayer);
        this._summary.add_child(this._countdown);
        this._button.add_child(this._summary);
        this._schedule = this._createMenu();
        this._actions = this._createMenu();
        this._button.connect('captured-event', (_actor: Clutter.Actor, event: Clutter.Event) =>
            this._handleEvent(event),
        );
        Main.panel.addToStatusArea(this.uuid, this._button, 0, this._position ?? 'right');
        this._button.container.hide();
        this._connectApp();
    }

    disable() {
        this._log('Disabled');
        this._connectionStatus = null;
        this._endDrag();
        this._clearTooltip();
        this._cancellable?.cancel();
        this._cancellable = null;
        if (this._proxy) {
            for (const signal of this._proxySignals) this._proxy.disconnect(signal);
        }
        this._proxy = null;
        this._state = null;
        if (this._busOwner) Gio.bus_unown_name(this._busOwner);
        this._busOwner = 0;
        this._schedule?.destroy();
        this._actions?.destroy();
        this._button?.destroy();
        this._moveArmed = false;
    }

    _createMenu() {
        const menu = new PopupMenu.PopupMenu(this._button, 0.5, St.Side.TOP);
        menu.actor.add_style_class_name('panel-menu');
        menu.actor.add_style_class_name('azkar-prayer-menu');
        (Main.uiGroup as Clutter.Actor).add_child(menu.actor);
        menu.actor.hide();
        // The shared panel manager opens sibling menus when the pointer crosses their source actors.
        const manager = new PopupMenu.PopupMenuManager(this._button);
        manager.addMenu(menu, 0);
        menu.connect('open-state-changed', (_, open) => {
            if (!open) this._clearTooltip();
            return undefined;
        });
        return menu;
    }

    _connectApp() {
        const cancellable = new Gio.Cancellable();
        this._cancellable = cancellable;
        Gio.DBusProxy.new(
            Gio.DBus.session,
            Gio.DBusProxyFlags.DO_NOT_AUTO_START,
            null,
            APP_BUS,
            PANEL_PATH,
            PANEL_INTERFACE,
            cancellable,
            (_obj, result) => {
                if (cancellable.is_cancelled()) return;
                try {
                    this._proxy = Gio.DBusProxy.new_finish(result);
                    this._proxySignals = [
                        this._proxy.connect('g-properties-changed', () => this._update()),
                        this._proxy.connect('notify::g-name-owner', () => this._update()),
                    ];
                    this._update();
                } catch (error) {
                    this._reportError(error, 'Connecting to the Azkar prayer panel');
                }
            },
        );
    }

    _update() {
        const appPresent = !!this._proxy?.get_name_owner();
        const value = appPresent ? this._proxy?.get_cached_property('Snapshot')?.unpack() : null;
        const previous = this._state;
        this._state = parsePrayerSnapshot(value);
        const state = this._state;
        const status = state
            ? 'Connected to Azkar'
            : appPresent
              ? 'Azkar is on D-Bus but has no compatible panel snapshot'
              : 'Waiting for Azkar on D-Bus';
        if (status !== this._connectionStatus) {
            this._log(status);
            this._connectionStatus = status;
        }
        if (state && previous?.hidden !== state.hidden) this._log(state.hidden ? 'Panel hidden' : 'Panel visible');
        if (state && !this._busOwner) {
            this._busOwner = Gio.bus_own_name_on_connection(
                Gio.DBus.session,
                EXTENSION_BUS,
                Gio.BusNameOwnerFlags.NONE,
                null,
                null,
            );
        } else if (!state && this._busOwner) {
            Gio.bus_unown_name(this._busOwner);
            this._busOwner = 0;
        }
        if (!state || previous?.hidden !== state.hidden) {
            this._moveArmed = false;
            this._endDrag();
            this._schedule.close();
            this._actions.close();
        }
        this._button.container.visible = !!state;
        if (!state) return;
        const themeChanged = JSON.stringify(previous?.palette) !== JSON.stringify(state.palette);
        if (themeChanged) {
            const p = state.palette;
            for (const menu of [this._schedule, this._actions]) {
                // The content box draws the rounded surface. An opaque BoxPointer behind it exposes square corners.
                menu.actor.style = p ? '-arrow-background-color: transparent; -arrow-border-width: 0;' : '';
                menu.box.style = p
                    ? `background-color: ${p.surface}; color: ${p.text}; border: 1px solid ${p.border}; border-radius: 12px;`
                    : '';
            }
        }
        if (previous) {
            for (const menu of [this._schedule, this._actions]) {
                if (!menu.isOpen) continue;
                const fields =
                    menu === this._actions
                        ? ([
                              'open',
                              'visibility',
                              'reminders',
                              'remindersPaused',
                              'quit',
                              'automatic',
                              'manual',
                              'dragHint',
                          ] as const)
                        : (['scheduleTitle', 'open', 'close'] as const);
                const changed =
                    fields.some((field) => previous[field] !== state[field]) ||
                    (menu !== this._actions && JSON.stringify(previous.schedule) !== JSON.stringify(state.schedule));
                if (changed || themeChanged) this._refreshMenu(menu, state);
            }
        }
        const showPrayer = !state.hidden && !!state.prayer;
        this._prayer.visible = showPrayer;
        this._prayer.text = showPrayer ? `${state.prayer} · ${state.time}` : '';
        this._countdown.visible = showPrayer;
        this._countdown.text = showPrayer ? state.remaining : '';
        this._button.accessible_name = showPrayer
            ? `${state.prayer}, ${state.time}, ${state.remaining}`
            : state.appName;
    }

    private _runAction(action: Action) {
        this._log(`Sending action to Azkar: ${action}`);
        this._proxy?.call(
            'RunAction',
            new GLib.Variant('(s)', [action]),
            Gio.DBusCallFlags.NO_AUTO_START,
            3000,
            this._cancellable,
            (proxy, result) => {
                try {
                    proxy?.call_finish(result);
                } catch (error) {
                    if (this._proxy === proxy) this._reportError(error, `Running Azkar action: ${action}`);
                }
            },
        );
    }

    private _toggleMenu(actions: boolean, input: 'mouse' | 'touch' | 'keyboard') {
        const state = this._state;
        if (!state) return;
        const menu = actions || state.hidden ? this._actions : this._schedule;
        const name = menu === this._schedule ? 'schedule' : 'actions';
        if (menu.isOpen) {
            this._log(`Closing ${name} menu via ${input}`);
            menu.close();
            return;
        }
        this._schedule.close();
        this._actions.close();
        menu.removeAll();
        if (menu === this._schedule) this._fillSchedule(menu, state);
        else this._fillActions(menu, state);
        this._log(`Opening ${name} menu via ${input}`);
        menu.open();
    }

    private _refreshMenu(menu: PopupMenu.PopupMenu, state: PrayerSnapshot) {
        // Restore keyboard focus to the corresponding control after rebuilding the menu.
        const path: number[] = [];
        let actor = (global.stage as Clutter.Stage).get_key_focus();
        while (actor && actor !== menu.actor) {
            const parent = actor.get_parent();
            if (!parent) break;
            path.unshift(parent.get_children().indexOf(actor));
            actor = parent;
        }
        const hadFocus = actor === menu.actor;
        this._clearTooltip();
        menu.removeAll();
        if (menu === this._actions) this._fillActions(menu, state);
        else this._fillSchedule(menu, state);
        if (hadFocus) {
            let target: Clutter.Actor | null = menu.actor;
            for (const index of path) target = target?.get_children()[index] ?? null;
            (target ?? menu.actor).grab_key_focus();
        }
    }

    private _fillSchedule(menu: PopupMenu.PopupMenu, state: PrayerSnapshot) {
        menu.box.add_style_class_name('azkar-prayer-schedule');
        const header = new PopupMenu.PopupBaseMenuItem({ reactive: false, can_focus: false });
        header.style_class = 'azkar-prayer-schedule-header';
        const title = new St.Label({
            text: state.scheduleTitle,
            style_class: 'azkar-prayer-heading',
            x_expand: true,
            y_align: Clutter.ActorAlign.CENTER,
        });
        header.add_child(new St.Icon({ gicon: this._panelIcon('calendar'), style_class: 'popup-menu-icon' }));
        if (state.palette) header.style = `color: ${state.palette.text}; border-color: ${state.palette.border};`;
        header.add_child(title);
        header.add_child(this._iconButton('x', state.close, () => menu.close()));
        header.add_child(
            this._iconButton('house', state.open, () => {
                menu.close();
                this._runAction('open');
            }),
        );
        menu.addMenuItem(header);

        for (const row of state.schedule) {
            const item = new PopupMenu.PopupBaseMenuItem({ reactive: false, can_focus: false });
            item.style_class = 'azkar-prayer-row';
            if (row.next) item.add_style_class_name('azkar-prayer-next');
            item.add_child(
                new St.Icon({
                    gicon: this._panelIcon(PRAYER_ICONS[row.key] ?? 'sun'),
                    style_class: 'popup-menu-icon',
                }),
            );
            item.add_child(new St.Label({ text: row.name, x_expand: true }));
            const time = new St.Label({ text: row.time, style_class: 'azkar-prayer-time' });
            if (state.palette) {
                const p = state.palette;
                item.style = row.next ? `color: ${p.text}; background-color: ${p.highlight};` : `color: ${p.text};`;
                time.style = `color: ${row.next ? p.accent : p.text};`;
            }
            item.add_child(time);
            menu.addMenuItem(item);
        }
    }

    private _fillActions(menu: PopupMenu.PopupMenu, state: PrayerSnapshot) {
        menu.box.add_style_class_name('azkar-prayer-actions');
        menu.addAction(state.open, () => this._runAction('open'), this._panelIcon('house'));
        if (state.visibility) {
            menu.addAction(
                state.visibility,
                () => this._runAction('visibility'),
                this._panelIcon(state.hidden ? 'visibility' : 'visibility-off'),
            );
        }
        menu.addAction(
            state.reminders,
            () => this._runAction('reminders'),
            this._panelIcon(state.remindersPaused ? 'bell' : 'bell-off'),
        );
        if (state.palette) {
            for (const item of menu.box.get_children()) (item as St.Widget).style = `color: ${state.palette.text};`;
        }
        menu.addMenuItem(new PopupMenu.PopupSeparatorMenuItem());
        const footer = new PopupMenu.PopupBaseMenuItem({ reactive: false, can_focus: false });
        if (state.palette) footer.style = `color: ${state.palette.text};`;
        footer.style_class = 'azkar-prayer-action-footer';
        const quitLabel = new St.BoxLayout({ style_class: 'azkar-prayer-quit-label' });
        quitLabel.add_child(new St.Icon({ gicon: this._panelIcon('x'), style_class: 'popup-menu-icon' }));
        quitLabel.add_child(new St.Label({ text: state.quit }));
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
        if (!state.hidden) this._addPositionControls(footer, menu, state);
        menu.addMenuItem(footer);
    }

    private _addPositionControls(row: PopupMenu.PopupBaseMenuItem, menu: PopupMenu.PopupMenu, state: PrayerSnapshot) {
        const automatic = this._iconButton('auto-awesome', state.automatic, () => {
            menu.close();
            this._moveArmed = false;
            this._place(null);
        });
        const manual = this._iconButton('open-with', `${state.manual}. ${state.dragHint}`, () => {
            menu.close();
            this._moveArmed = true;
            this._log('Manual positioning armed');
            this._button.grab_key_focus();
        });
        (this._position ? manual : automatic).add_style_class_name('azkar-prayer-position-selected');
        const controls = new St.BoxLayout({ style_class: 'azkar-prayer-position-controls' });
        controls.add_child(automatic);
        controls.add_child(manual);
        row.add_child(controls);
    }

    private _panelIcon(name: string) {
        // St and PopupMenu typings use different versions of Gio.Icon.
        return new Gio.FileIcon({ file: this.dir.get_child(`${name}-symbolic.svg`) }) as unknown as NonNullable<
            St.Icon.ConstructorProps['gicon'] & Parameters<PopupMenu.PopupMenu['addAction']>[2]
        >;
    }

    private _iconButton(icon: string, label: string, callback: () => void) {
        const button = new St.Button({
            child: new St.Icon({ gicon: this._panelIcon(icon), style_class: 'popup-menu-icon' }),
            style_class: 'azkar-prayer-icon-button',
            accessible_name: label,
            track_hover: true,
            can_focus: true,
        });
        button.connect('clicked', callback);
        button.connect('notify::hover', () => {
            if (button.hover) this._showTooltip(button, label);
            else this._clearTooltip();
        });
        return button;
    }

    private _showTooltip(anchor: Clutter.Actor, text: string) {
        this._clearTooltip();
        const tooltip = new St.Label({ text, style_class: 'azkar-prayer-tooltip' });
        this._tooltip = tooltip;
        (Main.uiGroup as Clutter.Actor).add_child(tooltip);
        const [x, y] = anchor.get_transformed_position();
        const [anchorWidth, anchorHeight] = anchor.get_transformed_size();
        const [, width] = tooltip.get_preferred_width(-1);
        const [, height] = tooltip.get_preferred_height(width);
        const monitor = Main.layoutManager.primaryMonitor;
        if (!monitor) {
            this._clearTooltip();
            return;
        }
        const left = Math.max(monitor.x, Math.min(x + (anchorWidth - width) / 2, monitor.x + monitor.width - width));
        tooltip.set_position(left, y - height - 4 >= monitor.y ? y - height - 4 : y + anchorHeight + 4);
    }

    _clearTooltip() {
        this._tooltip?.destroy();
        this._tooltip = null;
    }

    private _handleEvent(event: Clutter.Event) {
        if (this._drag) return this._dragEvent(event);
        const type = event.type();
        if (type === Clutter.EventType.BUTTON_PRESS) {
            if (
                (this._moveArmed || this._position) &&
                !this._state?.hidden &&
                event.get_button() === Clutter.BUTTON_PRIMARY
            )
                this._beginDrag(event.get_coords());
            else {
                this._moveArmed = false;
                this._toggleMenu(event.get_button() === Clutter.BUTTON_SECONDARY, 'mouse');
            }
            return Clutter.EVENT_STOP;
        }
        if (type === Clutter.EventType.TOUCH_BEGIN) {
            this._toggleMenu(false, 'touch');
            return Clutter.EVENT_STOP;
        }
        if (type === Clutter.EventType.KEY_PRESS) return this._keyPress(event);
        return Clutter.EVENT_PROPAGATE;
    }

    private _keyPress(event: Clutter.Event) {
        const key = event.get_key_symbol();
        if (this._moveArmed && (key === Clutter.KEY_Left || key === Clutter.KEY_Right)) {
            const index = POSITIONS.indexOf(this._position ?? 'right');
            this._place(POSITIONS[Math.max(0, Math.min(2, index + (key === Clutter.KEY_Left ? -1 : 1)))]);
            this._button.grab_key_focus();
        } else if (key === Clutter.KEY_Escape && this._moveArmed) {
            this._moveArmed = false;
        } else if (
            key === Clutter.KEY_Menu ||
            (key === Clutter.KEY_F10 && event.get_state() & Clutter.ModifierType.SHIFT_MASK)
        ) {
            this._toggleMenu(true, 'keyboard');
        } else if (key === Clutter.KEY_Return || key === Clutter.KEY_space) {
            this._toggleMenu(false, 'keyboard');
        } else {
            return Clutter.EVENT_PROPAGATE;
        }
        return Clutter.EVENT_STOP;
    }

    private _beginDrag([pressX]: [number, number]) {
        const [x, y] = this._summary.get_transformed_position();
        const [width, height] = this._summary.get_transformed_size();
        const stage = global.stage as Clutter.Stage;
        const grab = stage.grab(this._button);
        if (stage.get_grab_actor() !== this._button) {
            this._log('Could not grab pointer for panel move');
            grab.dismiss();
            this._moveArmed = false;
            return;
        }
        const preview = new Clutter.Clone({ source: this._summary, width, height, opacity: 220, reactive: false });
        preview.hide();
        (Main.uiGroup as Clutter.Actor).add_child(preview);
        (Main.uiGroup as Clutter.Actor).set_child_above_sibling(preview, null);
        preview.set_position(x, y);
        this._drag = { preview, grab, moved: false, pressX, offset: pressX - x, y };
    }

    private _dragEvent(event: Clutter.Event) {
        const drag = this._drag;
        if (!drag) return Clutter.EVENT_PROPAGATE;
        const type = event.type();
        if (type === Clutter.EventType.MOTION) {
            const [x] = event.get_coords();
            drag.moved ||= Math.abs(x - drag.pressX) > DRAG_THRESHOLD;
            if (drag.moved) {
                if (!drag.preview.visible) this._log('Dragging panel');
                drag.preview.set_position(x - drag.offset, drag.y);
                drag.preview.show();
                this._button.opacity = 0;
            }
        } else if (type === Clutter.EventType.BUTTON_RELEASE && event.get_button() === Clutter.BUTTON_PRIMARY) {
            const [x] = event.get_coords();
            const droppedAwayFromStart = Math.abs(x - drag.pressX) > DRAG_THRESHOLD;
            this._endDrag();
            if (droppedAwayFromStart) {
                const monitor = Main.layoutManager.primaryMonitor;
                if (!monitor) return Clutter.EVENT_STOP;
                this._place(positionAt(x, monitor.x, monitor.width));
            } else if (drag.moved) {
                this._log('Panel dropped at its starting point');
            } else {
                this._toggleMenu(false, 'mouse');
            }
        } else if (type === Clutter.EventType.KEY_PRESS && event.get_key_symbol() === Clutter.KEY_Escape) {
            this._log('Panel drag cancelled');
            this._endDrag();
        }
        return Clutter.EVENT_STOP;
    }

    _endDrag() {
        if (!this._drag) return;
        const { grab, preview } = this._drag;
        this._drag = null;
        this._moveArmed = false;
        preview.destroy();
        this._button.opacity = 255;
        grab.dismiss();
    }

    private _place(position: Position | null) {
        const target = Main.panel[`_${position ?? 'right'}Box`];
        if (!target) return;
        const container = this._button.container;
        if (container.get_parent() !== target) {
            container.get_parent()?.remove_child(container);
            target.insert_child_at_index(container, 0);
        }
        this._position = position;
        const path = this._positionPath();
        GLib.mkdir_with_parents(GLib.path_get_dirname(path), 0o700);
        try {
            GLib.file_set_contents(path, position ?? 'automatic');
            this._log(`Saved panel placement: ${position ?? 'automatic'}`);
        } catch (error) {
            this._reportError(error, 'Saving the Azkar panel position');
        }
    }

    private _positionPath() {
        return GLib.build_filenamev([GLib.get_user_config_dir(), 'azkar', 'gnome-panel-position']);
    }

    _readPosition() {
        const file = Gio.File.new_for_path(this._positionPath());
        try {
            const [, bytes] = file.load_contents(null);
            return parsePosition(new TextDecoder().decode(bytes));
        } catch (error) {
            if (!(error instanceof GLib.Error) || !error.matches(Gio.io_error_quark(), Gio.IOErrorEnum.NOT_FOUND))
                this._reportError(error, 'Reading the Azkar panel position');
            return null;
        }
    }
}
