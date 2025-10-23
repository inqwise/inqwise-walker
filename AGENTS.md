# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/inqwise/walker` hosts walker implementations such as `JsonObjectWalker` and context types.
- `src/main/generated` stores compiler output when annotation processors run; keep it out of version control.
- `src/test/java/com/inqwise/walker` contains JUnit 5 coverage; pair new classes with matching `*Test` files.
- `scripts/` includes release helpers (`release.sh`, `test-release.sh`) used by maintainers; keep them POSIX-friendly.

## Build, Test, and Development Commands
- `mvn clean install`: compiles sources, runs the full JUnit suite, and installs the snapshot locally.
- `mvn test`: fastest feedback loop; runs unit and integration tests under Vert.x with the configured Surefire settings.
- `mvn verify`: executes tests and produces JaCoCo coverage reports under `target/site/jacoco`.
- `scripts/test-release.sh`: smoke-tests the release workflow; run before invoking `scripts/release.sh` or opening a release PR.

## Coding Style & Naming Conventions
- Target Java 21 language features; use four-space indentation and keep lines under 120 characters to match existing code.
- Place public classes in the `com.inqwise.walker` namespace; favour descriptive class names like `ObjectWalkingContextImpl`.
- Prefer immutable fields where possible, and mark dependencies injected through constructors as `final`.
- Follow existing method naming (`handle`, `newSubItem`) and avoid inlined lambdas longer than ~5 lines.

## Testing Guidelines
- Tests use JUnit Jupiter with Vert.x helpers; stick to `@Test`, `@ParameterizedTest`, and the Vert.x extension for async flows.
- Name files `SomethingTest.java`, mirroring the class under test inside the same package tree.
- Ensure new behaviour is covered by unit tests plus integration coverage in `IntegrationTest`; keep JaCoCo line coverage near the current CI baseline.
- Run `mvn test` locally before pushing; inspect `target/site/jacoco/index.html` if coverage drops.

## Commit & Pull Request Guidelines
- Use concise, imperative commit subjects; scopes like `deps(deps):` are common for dependency bumps, otherwise favour `feat:`, `fix:`, or `chore:`.
- Reference related issues in the body, note any CI or release impacts, and keep commits focused on a single concern.
- Pull requests should describe the change set, outline test evidence (`mvn test`, screenshots for docs if relevant), and mention any new configuration steps.
- Request at least one review, ensure CI is green, and confirm changelog or release notes updates when altering public APIs.

## Security & Release Notes
- Review `SECURITY.md` before adding third-party libraries; prefer provided-scope dependencies like Vert.x and Log4j to minimise surface area.
- Keep secrets out of the repo and verify code scanning alerts from CodeQL, Snyk, and Codecov before merging.
- For Maven Central releases, follow the Sonatype profile in `pom.xml`; validate artifacts with `scripts/test-release.sh` prior to publishing.
