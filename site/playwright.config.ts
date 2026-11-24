import { defineConfig } from "@playwright/test";

const port = 4173;
const baseURL = `http://127.0.0.1:${port}`;

export default defineConfig({
  testDir: "./tests/e2e",
  use: { baseURL },
  webServer: {
    command: "bun run preview",
    env: { PORT: String(port) },
    url: `${baseURL}/download.html`,
  },
});
