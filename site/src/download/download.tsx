import type { ReactNode } from "react";
import { createRoot, hydrateRoot } from "react-dom/client";
import { Gallery } from "./gallery.js";
import { Installers } from "./installers.js";
import "./static-controls.js";

function mountSection(id: string, content: ReactNode) {
  const container = document.getElementById(id);
  if (!container) throw new Error(`Missing download page section: ${id}`);
  if (container.firstElementChild) {
    hydrateRoot(container, content);
  } else {
    createRoot(container).render(content);
  }
}

mountSection("installer-root", <Installers />);
mountSection("gallery-root", <Gallery />);
