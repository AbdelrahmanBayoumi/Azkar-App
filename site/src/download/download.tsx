import type { ReactNode } from "react";
import { hydrateRoot } from "react-dom/client";
import { Gallery } from "./gallery.js";
import { Installers } from "./installers.js";
import "./static-controls.js";

function hydrateSection(id: string, content: ReactNode) {
  const container = document.getElementById(id);
  if (!container) throw new Error(`Missing download page section: ${id}`);
  hydrateRoot(container, content);
}

hydrateSection("installer-root", <Installers />);
hydrateSection("gallery-root", <Gallery />);
