import {
  useRef,
  type ButtonHTMLAttributes,
  type KeyboardEvent,
  type ReactElement,
} from "react";

export type IconName =
  | "adjustments"
  | "arrow-up"
  | "bell"
  | "book"
  | "bookmark"
  | "clock"
  | "code"
  | "collection"
  | "compass"
  | "download"
  | "download-release"
  | "file-check"
  | "folder"
  | "grid"
  | "home"
  | "image"
  | "info"
  | "layers"
  | "library"
  | "library-category"
  | "linux"
  | "macos"
  | "monitor"
  | "moon"
  | "play"
  | "prayer"
  | "search"
  | "showcase"
  | "sliders"
  | "sun"
  | "tag"
  | "windows";

export function Icon({
  name,
  className,
}: {
  name: IconName;
  className?: string;
}): ReactElement {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
      <use href={`images/icons/${name}.svg#icon`} />
    </svg>
  );
}

export function tabIndexForKey(
  key: string,
  current: number,
  count: number,
): number | undefined {
  switch (key) {
    case "ArrowRight":
      return (current + 1) % count;
    case "ArrowLeft":
      return (current + count - 1) % count;
    case "Home":
      return 0;
    case "End":
      return count - 1;
    default:
      return undefined;
  }
}

type TabEvents = Pick<
  ButtonHTMLAttributes<HTMLButtonElement>,
  "onClick" | "onKeyDown"
> & {
  ref: (tab: HTMLButtonElement | null) => void;
};

export function useTabNavigation(
  count: number,
  select: (index: number) => void,
): (index: number) => TabEvents {
  const tabs = useRef<(HTMLButtonElement | null)[]>([]);

  return (index: number) => ({
    ref: (tab: HTMLButtonElement | null) => {
      tabs.current[index] = tab;
    },
    onClick: () => select(index),
    onKeyDown: (event: KeyboardEvent<HTMLButtonElement>) => {
      const next = tabIndexForKey(event.key, index, count);
      if (next === undefined) return;
      event.preventDefault();
      select(next);
      tabs.current[next]?.focus();
    },
  });
}
