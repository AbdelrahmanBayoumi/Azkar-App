import type { IconName } from "./ui.js";

export interface Screen {
  id: string;
  label: string;
  title: string;
  description: string;
  alt: string;
  highlights: readonly (readonly [title: string, details: string])[];
  icon: IconName;
}

export const screens: readonly Screen[] = [
  {
    id: "home",
    label: "Home",
    title: "Your day at a glance",
    description:
      "See today's remembrance and prayer times, with a countdown to the next prayer.",
    alt: "Azkar Home screen with today's remembrance, the prayer schedule, and a countdown to the next prayer",
    highlights: [
      [
        "Today's remembrance",
        "A featured passage is ready when you open Azkar.",
      ],
      ["Prayer schedule", "Check today's times and see which prayer is next."],
      ["Quick alerts", "Manage prayer notifications from the home screen."],
    ],
    icon: "home",
  },
  {
    id: "library",
    label: "Library",
    title: "Find your daily adhkar",
    description:
      "Browse by topic or search for a passage. Save the ones you return to.",
    alt: "Azkar collections overview with topic filters, search, and collection actions",
    highlights: [
      [
        "Browse collections",
        "Explore the bundled remembrance library by topic.",
      ],
      ["Search passages", "Find a collection or remembrance by its text."],
      ["Keep favorites", "Save passages you want to return to."],
    ],
    icon: "library",
  },
  {
    id: "reader",
    label: "Reading",
    title: "Read with context",
    description:
      "Read Arabic alongside its translation, with explanations and sources when available.",
    alt: "Azkar remembrance reader with Arabic text, translation, and source details",
    highlights: [
      [
        "Arabic and translation",
        "Read the original text with its translation.",
      ],
      ["Explanations", "See additional context where it is available."],
      ["Source references", "Check the reference attached to a passage."],
    ],
    icon: "book",
  },
  {
    id: "session",
    label: "Recitation",
    title: "Pick up where you left off",
    description:
      "Follow a collection one passage at a time, track repetitions, and resume your progress.",
    alt: "Azkar recitation session with passage navigation and progress",
    highlights: [
      ["One passage at a time", "Move through a collection at your own pace."],
      ["Track repetitions", "Keep count as you recite each remembrance."],
      ["Resume your place", "Continue your session when you return."],
    ],
    icon: "play",
  },
  {
    id: "collection",
    label: "Collections",
    title: "Keep your collections close",
    description:
      "Open a collection to read its passages, see your progress, or add it to your reminders.",
    alt: "An Azkar collection showing its passages, recitation progress, and reminder controls",
    highlights: [
      ["Read a collection", "Open its passages together in one place."],
      ["See your progress", "Keep track of what you have recited."],
      ["Set reminders", "Bring a collection back into your routine."],
    ],
    icon: "layers",
  },
  {
    id: "reminder",
    label: "Reminders",
    title: "A reminder during your day",
    description:
      "Read a passage without opening the main window, save it as a favorite, or dismiss it.",
    alt: "An Azkar passage reminder card with Arabic text, favorite and dismiss controls",
    highlights: [
      [
        "Read at a glance",
        "See a remembrance in a lightweight reminder window.",
      ],
      ["Save for later", "Add a useful passage to your favorites."],
      ["Stay in control", "Dismiss a reminder when you are finished."],
    ],
    icon: "bell",
  },
];
