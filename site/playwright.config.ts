import { defineConfig } from "@playwright/test";

const port = 4173;
const baseURL = `http://127.0.0.1:${port}`;

export default defineConfig({
  testDir: "./e2e",
  use: { baseURL },
  webServer: {
    command: "bun run dev",
    env: { PORT: String(port) },
    url: `${baseURL}/download.html`,
  },
});
