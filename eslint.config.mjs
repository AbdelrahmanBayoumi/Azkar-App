import { defineConfig, globalIgnores } from "eslint/config";
import tseslint from "typescript-eslint";

export default defineConfig(
  globalIgnores([
    "app/packaging/linux/gnomeExtension/dist/",
    "site/build/",
    "site/test-results/",
  ]),
  {
    files: [
      "app/packaging/linux/gnomeExtension/*.ts",
      "site/src/**/*.{ts,tsx}",
    ],
    extends: [tseslint.configs.recommendedTypeChecked],
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
  },
  {
    files: [
      "app/packaging/linux/gnomeExtension/tests/**/*.ts",
      "site/tests/**/*.ts",
      "site/*.config.ts",
    ],
    extends: [tseslint.configs.recommended],
  },
  {
    files: ["site/public/js/theme.js"],
    languageOptions: {
      globals: { document: "readonly", localStorage: "readonly" },
    },
    rules: { "no-undef": "error" },
  },
);
