# Repository Guidelines

## Project Structure & Module Organization

`Azkar/` is the Java 8/JavaFX Maven module; run commands there. Java lives in `src/main/java/com/bayoumi/`; `Launcher.java` is the entry point. Existing packages separate controllers, models, services, storage, and utilities. Resources hold FXML, CSS, images, fonts, language bundles, and `V<number>__description.sql` Flyway migrations. `jarFiles/` holds runtime assets; root `installer/` and `production.install4j` define installers.

## Build, Test, and Development Commands

Use Java 8 and Maven 3.3+; Scene Builder 8.5.0 is recommended for FXML. From `Azkar/`:

- `mvn -Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8 jfx:run` launches the app.
- `mvn -Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8 test` compiles and runs tests.
- `mvn -Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8 package` creates `target/Azkar.jar`.

The overrides are required because `jdk.version` is not connected to the compiler plugin in `pom.xml`.

## Coding Style & Testing

Use four spaces and same-line opening braces. Types are PascalCase, methods and fields lowerCamelCase, and constants UPPER_SNAKE_CASE. Preserve suffixes `Controller`, `Service`, `Manager`, `Util`, and `DTO`; use lowerCamelCase FXML IDs and handlers. No formatter is enforced; DeepSource analyzes Java 8.

No test framework or coverage threshold is configured. Add test dependencies to `pom.xml`, mirror packages under `src/test/java/`, and name tests `*Test.java`. Manually verify affected screens in both languages and relevant light/dark themes.

## Agent Workflow & Devil's Advocate

Before any action, perform a Devil's Advocate pass: challenge assumptions, separate verified facts from inference, identify failure modes and side effects, and consider a simpler or safer alternative. Surface material findings; keep routine execution concise.

- Inspect relevant code, tests, documentation, and configuration first; reuse established patterns and utilities.
- Do not edit for analysis or review. Get explicit confirmation before implementation and fresh approval before destructive commands, mass changes, resets, force pushes, or database changes.
- Keep changes narrowly scoped and preserve behavior unless a refactor is requested. Justify every new dependency or abstraction.
- Never edit generated `Azkar/target/` content. Run the narrowest relevant verification, then report what changed, why, material risks, and results.
- Verify every documented symbol, command, and path against the repository.

## Commit & Pull Request Guidelines

Keep commit subjects action-oriented and at most 50 characters; Conventional prefixes are optional. Reference issues when relevant. Pull requests should summarize behavior and verification, link issues, and show before/after images for FXML or CSS changes. No PR template exists.

## Configuration & Secrets

Copy `config.example.properties` and `sentry.example.properties` to their non-example names under `src/main/resources/`. These local files are ignored; never commit API keys, server URLs, or Sentry DSNs.
