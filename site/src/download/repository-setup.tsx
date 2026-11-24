import { useEffect, useRef, useState } from "react";
import { repositoryCommands } from "./repository-commands.js";

const packageManagers = Object.keys(
  repositoryCommands,
) as (keyof typeof repositoryCommands)[];

export function RepositorySetup({ visible }: { visible: boolean }) {
  const [repository, setRepository] =
    useState<keyof typeof repositoryCommands>("apt");
  const [copyLabel, setCopyLabel] = useState("Copy");
  const code = useRef<HTMLElement>(null);
  const copyRequest = useRef(0);
  const copyTimer = useRef<number | undefined>(undefined);
  useEffect(
    () => () => {
      copyRequest.current++;
      window.clearTimeout(copyTimer.current);
    },
    [],
  );

  async function copyCommands() {
    const request = ++copyRequest.current;
    window.clearTimeout(copyTimer.current);
    try {
      await navigator.clipboard.writeText(repositoryCommands[repository]);
      if (request !== copyRequest.current) return;
      setCopyLabel("Copied");
      copyTimer.current = window.setTimeout(() => setCopyLabel("Copy"), 1800);
    } catch {
      if (request !== copyRequest.current) return;
      const selection = window.getSelection();
      if (code.current && selection) {
        const range = document.createRange();
        range.selectNodeContents(code.current);
        selection.removeAllRanges();
        selection.addRange(range);
      }
      setCopyLabel("Copy manually");
    }
  }

  return (
    <details
      className="linux-repositories"
      id="linux-repositories"
      hidden={!visible}
    >
      <summary>Install from the signed repository</summary>
      <div className="detail-body">
        <div className="repository-command">
          <div className="repository-toolbar">
            <div
              className="repository-tabs"
              role="group"
              aria-label="Package manager"
            >
              {packageManagers.map((name) => (
                <button
                  key={name}
                  type="button"
                  className="repository-tab"
                  data-repository={name}
                  aria-pressed={repository === name}
                  onClick={() => {
                    copyRequest.current++;
                    window.clearTimeout(copyTimer.current);
                    setRepository(name);
                    setCopyLabel("Copy");
                  }}
                >
                  {name.toUpperCase()}
                </button>
              ))}
            </div>
            <button
              className="copy"
              id="repository-copy"
              type="button"
              aria-live="polite"
              onClick={() => void copyCommands()}
            >
              {copyLabel}
            </button>
          </div>
          {packageManagers.map((name) => (
            <div
              key={name}
              className="code-wrap"
              data-repository-panel={name}
              hidden={repository !== name}
            >
              <pre id={`${name}-commands`}>
                <code ref={repository === name ? code : undefined}>
                  {repositoryCommands[name]}
                </code>
              </pre>
            </div>
          ))}
        </div>
      </div>
    </details>
  );
}
