import type St from 'gi://St';

// GNOME Shell 46 declarations omit these members used by the panel and its menus.
declare module '@girs/gnome-shell/ui/panel' {
    interface Panel {
        readonly menuManager: import('@girs/gnome-shell/ui/popupMenu').PopupMenuManager;
        readonly _leftBox: St.BoxLayout;
        readonly _centerBox: St.BoxLayout;
        readonly _rightBox: St.BoxLayout;
    }
}

declare module '@girs/gnome-shell/ui/popupMenu' {
    namespace PopupMenuBase {
        interface SignalMap {
            'open-state-changed': [open: boolean];
        }
    }

    interface PopupMenuBase {
        addAction(title: string, callback: () => void, icon: string): void;
    }

    interface PopupMenu {
        open(): void;
        close(): void;
    }

    interface PopupMenuManager {
        readonly activeMenu: PopupMenu | null;
        addMenu(menu: PopupMenuBase): void;
    }
}
