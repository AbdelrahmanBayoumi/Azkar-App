import { useEffect, useRef, useState } from "react";

export function TerminalCommand({
  command,
  label,
}: {
  command: string;
  label?: string;
}) {
  const code = useRef<HTMLElement>(null);
  const [copyLabel, setCopyLabel] = useState("Copy");
  const copyRequest = useRef(0);
  const copyTimer = useRef<number | undefined>(undefined);
  useEffect(
    () => () => {
      copyRequest.current++;
      window.clearTimeout(copyTimer.current);
    },
    [],
  );

  async function copyCommand() {
    const request = ++copyRequest.current;
    window.clearTimeout(copyTimer.current);
    try {
      await navigator.clipboard.writeText(command);
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

  const commandText = <code ref={code}>{command}</code>;
  const copyButton = (
    <button
      className="copy"
      type="button"
      aria-live="polite"
      onClick={() => void copyCommand()}
    >
      {copyLabel}
    </button>
  );

  if (label === undefined) {
    return (
      <span className="inline-command">
        {commandText}
        {copyButton}
      </span>
    );
  }

  return (
    <div className="terminal-command">
      <div className="terminal-toolbar">
        <span>{label}</span>
        {copyButton}
      </div>
      <pre>{commandText}</pre>
    </div>
  );
}
