import { cpSync, mkdirSync, rmSync } from "node:fs";
import { renderToString } from "react-dom/server";
import { Gallery } from "./download/gallery.js";
import { Installers } from "./download/installers.js";

const output = "build";
const template = await Bun.file("src/download.html").text();
const html = template
  .replace("<!-- installers -->", () => renderToString(<Installers />))
  .replace("<!-- gallery -->", () => renderToString(<Gallery />));

rmSync(output, { recursive: true, force: true });
mkdirSync(`${output}/js`, { recursive: true });
const result = await Bun.build({
  entrypoints: ["src/download/download.tsx"],
  outdir: `${output}/js`,
  target: "browser",
  minify: true,
  define: { "process.env.NODE_ENV": '"production"' },
});
if (!result.success)
  throw new AggregateError(result.logs, "Could not build the download site");

cpSync("public", output, { recursive: true });
await Bun.write(`${output}/download.html`, html);
