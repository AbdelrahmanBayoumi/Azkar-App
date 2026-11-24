import { defineConfig, globalIgnores } from 'eslint/config';
import tseslint from 'typescript-eslint';

export default defineConfig(
    globalIgnores(['dist/']),
    {
        files: ['*.ts'],
        extends: [tseslint.configs.recommendedTypeChecked],
        languageOptions: {
            parserOptions: {
                projectService: true,
                tsconfigRootDir: import.meta.dirname,
            },
        },
    },
    {
        files: ['tests/**/*.ts'],
        extends: [tseslint.configs.recommended],
    },
);
