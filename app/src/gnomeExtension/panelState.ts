interface ScheduleRow {
    key: string;
    name: string;
    time: string;
    next: boolean;
}

// Serialized by ShellPanelSnapshot in GnomeShellPrayerPanel.kt.
export interface PrayerSnapshot {
    palette?: { surface: string; text: string; border: string; highlight: string; accent: string };
    hidden: boolean;
    appName: string;
    prayer: string;
    time: string;
    remaining: string;
    scheduleTitle: string;
    schedule: ScheduleRow[];
    open: string;
    visibility: string;
    reminders: string;
    remindersPaused: boolean;
    quit: string;
    close: string;
    automatic: string;
    manual: string;
    dragHint: string;
}

const SNAPSHOT_TEXT_FIELDS = [
    'appName',
    'prayer',
    'time',
    'remaining',
    'scheduleTitle',
    'open',
    'visibility',
    'reminders',
    'quit',
    'close',
    'automatic',
    'manual',
    'dragHint',
] satisfies (keyof PrayerSnapshot)[];

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
}

function isScheduleRow(row: unknown): row is ScheduleRow {
    return (
        isRecord(row) &&
        typeof row.key === 'string' &&
        typeof row.name === 'string' &&
        typeof row.time === 'string' &&
        typeof row.next === 'boolean'
    );
}

function isSnapshot(value: unknown): value is PrayerSnapshot {
    return (
        isRecord(value) &&
        typeof value.hidden === 'boolean' &&
        typeof value.remindersPaused === 'boolean' &&
        SNAPSHOT_TEXT_FIELDS.every((field) => typeof value[field] === 'string') &&
        Array.isArray(value.schedule) &&
        value.schedule.every(isScheduleRow)
    );
}

export function parsePrayerSnapshot(value: unknown): PrayerSnapshot | null {
    if (typeof value !== 'string') return null;
    try {
        const snapshot: unknown = JSON.parse(value);
        if (!isSnapshot(snapshot)) return null;
        const palette: unknown = snapshot.palette;
        if (
            palette !== undefined &&
            !(
                isRecord(palette) &&
                ['surface', 'text', 'border', 'highlight', 'accent'].every(
                    (key) => typeof palette[key] === 'string' && /^#[0-9a-f]{6}$/i.test(palette[key]),
                )
            )
        )
            delete snapshot.palette;
        return snapshot;
    } catch {
        return null;
    }
}

export const POSITIONS = ['left', 'center', 'right'] as const;
export type Position = (typeof POSITIONS)[number];

export function parsePosition(saved: string): Position | null {
    const position = saved.trim();
    return POSITIONS.find((candidate) => candidate === position) ?? null;
}

export function positionAt(center: number, monitorX: number, monitorWidth: number): Position {
    const section = Math.floor((3 * (center - monitorX)) / monitorWidth);
    return POSITIONS[Math.max(0, Math.min(POSITIONS.length - 1, section))];
}
