import { useEffect, useState, type FocusEvent } from "react";
import { screens, type Screen } from "./screens.js";
import { TerminalCommand } from "./terminal-command.js";
import { activeTheme } from "./theme.js";
import { Icon, useTabNavigation } from "./ui.js";

function Screenshot({
  screen,
  selected,
  dark,
  onError,
}: {
  screen: Screen;
  selected: boolean;
  dark: boolean | undefined;
  onError: () => void;
}) {
  const source =
    selected && dark !== undefined
      ? `images/screen-${screen.id}-${dark ? "dark" : "light"}.webp`
      : undefined;

  return (
    <figure
      className="screenshot-card"
      id={`screen-panel-${screen.id}`}
      role="tabpanel"
      aria-labelledby={`screen-tab-${screen.id}`}
      tabIndex={0}
      hidden={!selected}
    >
      <img
        src={source}
        width={1440}
        height={900}
        loading="lazy"
        decoding="async"
        alt={screen.alt}
        onError={onError}
      />
      <figcaption>
        <div className="screenshot-caption-copy">
          <h3>{screen.title}</h3>
          <p>{screen.description}</p>
        </div>
        <ul
          className="screenshot-highlights"
          aria-label={`${screen.label} screen highlights`}
        >
          {screen.highlights.map(([title, details]) => (
            <li key={title}>
              <strong>{title}</strong>
              <span>{details}</span>
            </li>
          ))}
        </ul>
      </figcaption>
    </figure>
  );
}

export function Gallery() {
  const [selectedIndex, select] = useState(0);
  const tabEvents = useTabNavigation(screens.length, select);
  const [paused, setPaused] = useState(false);
  const [hidden, setHidden] = useState(false);
  const [reduceMotion, setReduceMotion] = useState(true);
  const [dark, setDark] = useState<boolean>();
  const [missing, setMissing] = useState(false);

  useEffect(() => {
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    const systemDark = window.matchMedia("(prefers-color-scheme: dark)");
    const hero = document.querySelector<HTMLImageElement>(".hero-preview img");
    const syncMotion = () => setReduceMotion(reducedMotion.matches);
    const syncVisibility = () => setHidden(document.hidden);
    const syncTheme = () => setDark(activeTheme() === "dark");
    const warn = () => setMissing(true);
    const theme = new MutationObserver(syncTheme);
    theme.observe(document.documentElement, {
      attributeFilter: ["data-theme"],
    });
    hero?.addEventListener("error", warn);
    if (hero?.hasAttribute("src") && hero.complete && hero.naturalWidth === 0)
      warn();
    reducedMotion.addEventListener("change", syncMotion);
    systemDark.addEventListener("change", syncTheme);
    document.addEventListener("visibilitychange", syncVisibility);
    syncMotion();
    syncVisibility();
    syncTheme();
    return () => {
      theme.disconnect();
      hero?.removeEventListener("error", warn);
      reducedMotion.removeEventListener("change", syncMotion);
      systemDark.removeEventListener("change", syncTheme);
      document.removeEventListener("visibilitychange", syncVisibility);
    };
  }, []);

  function advance() {
    select((index) => (index + 1) % screens.length);
  }

  function syncFocus(event: FocusEvent<HTMLDivElement>) {
    const next = event.type === "blur" ? event.relatedTarget : event.target;
    setPaused(
      next instanceof Element &&
        event.currentTarget.contains(next) &&
        next.matches(":focus-visible"),
    );
  }

  return (
    <>
      <div
        className="screenshot-warning"
        id="screenshot-warning"
        hidden={!missing}
      >
        <Icon name="info" />
        <p>
          <span role="status">Screenshot unavailable.</span> Run{" "}
          <TerminalCommand command="./gradlew :app:captureDownloadScreenshots" />{" "}
          from the project root, then rebuild the site.
        </p>
      </div>
      <div
        className={`screen-tabs${paused || hidden ? " is-paused" : ""}`}
        role="tablist"
        aria-label="Explore app screens"
        onFocus={syncFocus}
        onBlur={syncFocus}
      >
        {screens.map((screen, index) => {
          const selected = index === selectedIndex;
          return (
            <button
              key={screen.id}
              className={`screen-tab${selected && !reduceMotion ? " is-counting" : ""}`}
              id={`screen-tab-${screen.id}`}
              type="button"
              role="tab"
              aria-controls={`screen-panel-${screen.id}`}
              aria-selected={selected}
              tabIndex={selected ? 0 : -1}
              {...tabEvents(index)}
            >
              <Icon name={screen.icon} />
              {screen.label}
              <span className="screen-tab-progress" aria-hidden="true">
                <span
                  className="screen-tab-progress-fill"
                  onAnimationEnd={selected ? advance : undefined}
                />
              </span>
            </button>
          );
        })}
      </div>
      <div className="screen-panels">
        {screens.map((screen, index) => (
          <Screenshot
            key={screen.id}
            screen={screen}
            selected={index === selectedIndex}
            dark={dark}
            onError={() => setMissing(true)}
          />
        ))}
      </div>
    </>
  );
}
