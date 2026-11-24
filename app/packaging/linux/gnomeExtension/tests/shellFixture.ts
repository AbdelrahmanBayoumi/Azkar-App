import { mock } from 'bun:test';
import type { PrayerSnapshot } from '../panelState.js';

class Actor {
    children: Actor[] = [];
    style = '';
    get_children() {
        return this.children;
    }
    visible = true;
    hover = false;
    text?: string;
    private handlers = new Map<number, [string, (...args: unknown[]) => void]>();
    private nextId = 0;

    constructor(props: { text?: string; child?: Actor } = {}) {
        this.text = props.text;
        if (props.child) this.add_child(props.child);
    }

    connect(name: string, handler: (...args: unknown[]) => void) {
        const id = ++this.nextId;
        this.handlers.set(id, [name, handler]);
        return id;
    }

    disconnect(id: number) {
        this.handlers.delete(id);
    }

    emit(name: string, ...args: unknown[]) {
        for (const [signal, handler] of this.handlers.values()) {
            if (signal === name) handler(this, ...args);
        }
    }

    add_child(child: Actor) {
        this.children.push(child);
    }

    get_transformed_position() {
        return [0, 0];
    }
    get_transformed_size() {
        return [100, 20];
    }
    set_position() {}
    set_child_above_sibling() {}
    add_style_class_name() {}

    show() {
        this.visible = true;
    }

    hide() {
        this.visible = false;
    }

    labels(): string[] {
        if (!this.visible) return [];
        return [...(this.text === undefined ? [] : [this.text]), ...this.children.flatMap((child) => child.labels())];
    }

    destroy() {
        this.hide();
    }
}

class Indicator extends Actor {
    container = new Actor();
}

class Menu extends Actor {
    actor = new Actor();
    box = new Actor();
    isOpen = false;
    actions = new Map<string, () => void>();

    open() {
        this.isOpen = true;
    }

    close() {
        this.isOpen = false;
        this.emit('open-state-changed', false);
    }

    removeAll() {
        this.box.children = [];
        this.actions.clear();
    }

    addAction(label: string, action: () => void) {
        this.actions.set(label, action);
        this.box.add_child(new Actor({ text: label }));
    }

    addMenuItem(item: Actor) {
        this.box.add_child(item);
    }
}

class Proxy extends Actor {
    snapshot: PrayerSnapshot | null = null;

    get_name_owner() {
        return this.snapshot ? ':1.1' : null;
    }

    get_cached_property() {
        return { unpack: () => JSON.stringify(this.snapshot) };
    }

    call(method: string, variant: Variant) {
        if (method === 'LogEvent') {
            shell.logs.push(variant.fields);
            return;
        }
        if (method !== 'RunAction') throw new Error(`Unexpected D-Bus call ${method}`);
        shell.calls.push(variant.fields[0]);
    }
}

class Variant {
    constructor(
        _signature: string,
        readonly fields: string[],
    ) {}
}

const menus: Menu[] = [];
let proxy: Proxy;
const menuManager = {
    get activeMenu() {
        return menus.find((menu) => menu.isOpen);
    },
    addMenu(menu: Menu) {
        menus.push(menu);
    },
};
const uiGroup = new Actor();

export const shell = {
    indicator: null as Indicator | null,
    calls: [] as string[],
    logs: [] as string[][],
    ready: false,
    sharedMenus: [] as Menu[],
    savedPosition: '',
    grabbed: false,
    pointerGrabAvailable: true,
    reset() {
        this.indicator = null;
        this.calls.length = 0;
        this.logs.length = 0;
        this.ready = false;
        this.savedPosition = '';
        this.pointerGrabAvailable = true;
        Object.assign(globalThis, {
            stage: {
                get_key_focus: () => null,
                get_grab_actor: () => (this.grabbed && this.pointerGrabAvailable ? this.indicator : null),
                grab: () => {
                    this.grabbed = true;
                    return {
                        dismiss: () => {
                            this.grabbed = false;
                        },
                    };
                },
            },
        });

        uiGroup.children.length = 0;
        menus.length = 0;
        this.sharedMenus.length = 0;
        allMenus.length = 0;
        proxy = new Proxy();
    },
    publish(value: PrayerSnapshot | null) {
        proxy.snapshot = value;
        proxy.emit('g-properties-changed');
    },
    hover(hovered: boolean) {
        if (!this.indicator) throw new Error('The indicator is missing');
        this.indicator.hover = hovered;
        this.indicator.emit('notify::hover');
    },
    pointer(type: string, x: number) {
        this.indicator?.emit('captured-event', { type: () => type, get_button: () => 1, get_coords: () => [x, 0] });
    },
    press(button: number) {
        this.indicator?.emit('captured-event', { type: () => 'press', get_button: () => button });
    },
    get menuStyle() {
        return menuManager.activeMenu?.box.style;
    },
    visibleLabels() {
        return allMenus.filter((menu) => menu.isOpen).flatMap((menu) => menu.box.labels());
    },
    dismissMenu() {
        menuManager.activeMenu?.close();
    },
    activate(label: string) {
        const action = menuManager.activeMenu?.actions.get(label);
        if (!action) throw new Error(`Missing menu action ${label}`);
        action();
    },
};

const allMenus: Menu[] = [];

mock.module('gi://Clutter', () => ({
    default: {
        ActorAlign: { CENTER: 1, FILL: 2 },
        BUTTON_PRIMARY: 1,
        BUTTON_SECONDARY: 3,
        Clone: Actor,
        EventType: { BUTTON_PRESS: 'press', BUTTON_RELEASE: 'release', MOTION: 'motion' },
    },
}));

mock.module('gi://Gio', () => ({
    default: {
        File: { new_for_path: () => ({ load_contents: () => [true, new TextEncoder().encode(shell.savedPosition)] }) },
        FileIcon: Actor,
        ThemedIcon: { new_from_names: () => ({}) },
        Cancellable: class {
            cancel() {}
            is_cancelled() {
                return false;
            }
        },
        DBus: { session: {} },
        DBusProxyFlags: { DO_NOT_AUTO_START: 1 },
        DBusCallFlags: { NO_AUTO_START: 1 },
        DBusProxy: {
            new: (...args: unknown[]) => {
                const callback = args.at(-1);
                if (typeof callback === 'function') callback(null, null);
            },
            new_finish: () => proxy,
        },
        BusNameOwnerFlags: { NONE: 0 },
        bus_own_name_on_connection: () => {
            shell.ready = true;
            return 1;
        },
        bus_unown_name: () => {
            shell.ready = false;
        },
    },
}));

mock.module('gi://GLib', () => ({
    default: { Variant, get_user_config_dir: () => '/config', build_filenamev: (paths: string[]) => paths.join('/') },
}));

mock.module('gi://St', () => ({
    default: { BoxLayout: Actor, Button: Actor, Icon: Actor, Label: Actor, Side: { TOP: 0 } },
}));

mock.module('resource:///org/gnome/shell/extensions/extension.js', () => ({
    Extension: class {
        uuid = 'azkar@test';
        metadata = { name: 'Azkar' };
        dir = { get_child: (name: string) => name };
    },
}));

mock.module('resource:///org/gnome/shell/ui/main.js', () => ({
    uiGroup,
    layoutManager: { primaryMonitor: { x: 0, width: 900 } },
    panel: {
        addToStatusArea: (_uuid: string, indicator: Indicator) => {
            shell.indicator = indicator;
        },
        menuManager: { addMenu: (menu: Menu) => shell.sharedMenus.push(menu) },
    },
}));

mock.module('resource:///org/gnome/shell/ui/panelMenu.js', () => ({ Button: Indicator }));

mock.module('resource:///org/gnome/shell/ui/popupMenu.js', () => ({
    PopupMenuManager: class {
        addMenu = menuManager.addMenu;
    },
    PopupMenu: class extends Menu {
        constructor() {
            super();
            allMenus.push(this);
        }
    },
    PopupBaseMenuItem: Actor,
    PopupSeparatorMenuItem: Actor,
}));
