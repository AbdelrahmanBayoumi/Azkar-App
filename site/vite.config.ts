import { fileURLToPath } from "node:url";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  root: "src",
  base: "./",
  appType: "mpa",
  publicDir: "../public",
  cacheDir: "../node_modules/.vite",
  plugins: [react()],
  build: {
    outDir: "../build",
    emptyOutDir: true,
    rolldownOptions: {
      output: {
        // The Pages publisher expects these asset paths.
        entryFileNames: "js/download.js",
        chunkFileNames: "js/[name]-[hash].js",
        assetFileNames: "css/download[extname]",
      },
    },
  },
  server: {
    host: "127.0.0.1",
    port: Number(process.env.PORT ?? 5173),
  },
  preview: {
    port: Number(process.env.PORT ?? 3000),
    strictPort: true,
  },
  test: {
    dir: fileURLToPath(new URL("./tests/unit", import.meta.url)),
    // Use worker threads for compatibility with Bun.
    pool: "threads",
  },
});
