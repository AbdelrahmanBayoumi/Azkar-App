import { afterEach, beforeEach, expect, test } from 'bun:test';
import { snapshot } from './prayerSnapshot.js';
import { shell } from './shellFixture.js';

const { default: AzkarPrayerPanel } = await import('../extension.js');
let extension: InstanceType<typeof AzkarPrayerPanel>;

beforeEach(() => {
    shell.reset();
    extension = new AzkarPrayerPanel({ name: 'Azkar', uuid: 'azkar@test' } as ConstructorParameters<
        typeof AzkarPrayerPanel
    >[0]);
    extension.enable();
});

afterEach(() => extension.disable());

test('shows the current prayer and updates the countdown when Azkar publishes a new snapshot', () => {
    expect(shell.indicator?.container.visible).toBe(false);

    shell.publish(snapshot);
    expect(shell.indicator?.container.visible).toBe(true);
    expect(shell.indicator?.labels()).toEqual(['Fajr · 5:15 AM', '12 min']);

    shell.publish({ ...snapshot, remaining: '11 min' });
    expect(shell.indicator?.labels()).toEqual(['Fajr · 5:15 AM', '11 min']);

    shell.publish(null);
    expect(shell.indicator?.container.visible).toBe(false);
});

test('opens the schedule on primary click and keeps it open when the pointer leaves', () => {
    shell.publish(snapshot);
    shell.hover(true);
    expect(shell.visibleLabels()).toEqual([]);
    shell.press(1);
    expect(shell.visibleLabels()).toContain('5:15 AM');
    expect(shell.logs).toContainEqual(['info', 'Opening schedule menu via mouse']);
    shell.hover(false);
    expect(shell.visibleLabels()).toContain('Prayer times');
    shell.dismissMenu();
    shell.hover(true);
    expect(shell.visibleLabels()).toEqual([]);
    expect(shell.sharedMenus).toHaveLength(0);
});

test('secondary click opens the action menu and sends a reminder action to Azkar', () => {
    shell.publish(snapshot);
    shell.press(3);
    expect(shell.visibleLabels()).toContain('Pause reminders');
    expect(shell.visibleLabels()).not.toContain('Prayer times');

    shell.activate('Pause reminders');
    expect(shell.calls).toEqual(['reminders']);
});

test('updates schedule times in the open schedule', () => {
    shell.publish(snapshot);
    shell.press(1);
    shell.publish({ ...snapshot, schedule: [{ ...snapshot.schedule[0], time: '6:00 AM' }] });
    expect(shell.visibleLabels()).toContain('6:00 AM');
    expect(shell.visibleLabels()).not.toContain('5:15 AM');
});

test('updates the reminder action while the context menu is open', () => {
    shell.publish(snapshot);
    shell.press(3);
    shell.publish({ ...snapshot, remindersPaused: true, reminders: 'Resume reminders' });
    expect(shell.visibleLabels()).not.toContain('Pause reminders');
    shell.activate('Resume reminders');
    expect(shell.calls).toEqual(['reminders']);
});

test('claims readiness only while it has a compatible snapshot', () => {
    expect(shell.ready).toBe(false);
    shell.publish(snapshot);
    expect(shell.ready).toBe(true);
    shell.publish({ ...snapshot, schedule: null } as unknown as typeof snapshot);
    expect(shell.ready).toBe(false);
    shell.publish(snapshot);
    expect(shell.ready).toBe(true);
    shell.publish(null);
    expect(shell.ready).toBe(false);
});

test('returning to the press point during a drag does not open the schedule', () => {
    extension.disable();
    shell.savedPosition = 'left';
    extension.enable();
    shell.publish(snapshot);
    shell.pointer('press', 30);
    expect(shell.grabbed).toBe(true);
    shell.pointer('motion', 200);
    shell.pointer('release', 30);
    expect(shell.grabbed).toBe(false);
    expect(shell.visibleLabels()).toEqual([]);
    shell.pointer('press', 30);
    shell.pointer('release', 32);
    expect(shell.visibleLabels()).toContain('Prayer times');
});

test('a failed pointer grab leaves the panel available for another click', () => {
    extension.disable();
    shell.savedPosition = 'left';
    extension.enable();
    shell.publish(snapshot);
    shell.pointerGrabAvailable = false;
    shell.pointer('press', 30);
    expect(shell.grabbed).toBe(false);
    shell.pointerGrabAvailable = true;
    shell.pointer('press', 30);
    shell.pointer('release', 30);
    expect(shell.visibleLabels()).toContain('Prayer times');
});

test.each([1, 3])('updates the open menu palette on button %i', (button) => {
    const palette = { surface: '#ffffff', text: '#121212', highlight: '#dddddd', accent: '#234567', border: '#8899aa' };
    shell.publish({ ...snapshot, palette });
    shell.press(button);
    expect(shell.menuStyle).toContain('#ffffff');
    shell.publish({ ...snapshot, palette: { ...palette, surface: '#121b2e' } });
    expect(shell.menuStyle).toContain('#121b2e');
});
