import { renderToString } from "react-dom/server";
import { Gallery } from "./download/gallery.js";
import { Installers } from "./download/installers.js";

const index = "build/index.html";
const template = await Bun.file(index).text();
const installers = "<!-- installers -->";
const gallery = "<!-- gallery -->";
if (!template.includes(installers) || !template.includes(gallery)) {
  throw new Error("Missing React sections in the built download page");
}

const html = template
  .replace(installers, () => renderToString(<Installers />))
  .replace(gallery, () => renderToString(<Gallery />));

await Bun.write(index, html);
await Bun.write("build/download.html", html);
