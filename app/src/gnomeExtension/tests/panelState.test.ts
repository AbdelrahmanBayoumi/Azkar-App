import { expect, test } from 'bun:test';
import { parsePosition, parsePrayerSnapshot, positionAt } from '../panelState.js';

import { snapshot } from './prayerSnapshot.js';

test('decodes a snapshot from Azkar', () => {
    expect(parsePrayerSnapshot(JSON.stringify(snapshot))).toEqual(snapshot);
});

test('accepts a hidden panel with no prayer schedule or visibility action', () => {
    const hidden = { ...snapshot, hidden: true, prayer: '', time: '', remaining: '', schedule: [], visibility: '' };
    expect(parsePrayerSnapshot(JSON.stringify(hidden))).toEqual(hidden);
});

test('rejects malformed and incomplete snapshots', () => {
    expect(parsePrayerSnapshot(null)).toBeNull();
    expect(parsePrayerSnapshot('{')).toBeNull();
    expect(parsePrayerSnapshot(JSON.stringify({ ...snapshot, hidden: undefined }))).toBeNull();
    expect(
        parsePrayerSnapshot(JSON.stringify({ ...snapshot, schedule: [{ ...snapshot.schedule[0], next: 'yes' }] })),
    ).toBeNull();
});

test('restores a saved position or uses automatic placement', () => {
    expect(parsePosition(' center\n')).toBe('center');
    expect(parsePosition('automatic')).toBeNull();
    expect(parsePosition('invalid')).toBeNull();
});

test('snaps to panel sections and clamps drops beyond a monitor with a nonzero origin', () => {
    expect(positionAt(0, 300, 900)).toBe('left');
    expect(positionAt(599, 300, 900)).toBe('left');
    expect(positionAt(600, 300, 900)).toBe('center');
    expect(positionAt(900, 300, 900)).toBe('right');
    expect(positionAt(1500, 300, 900)).toBe('right');
});
