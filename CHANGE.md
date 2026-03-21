# CHANGE

All notable changes to this project will be documented in this file.

## [1.0.1] - 2026-03-22

### Added
- Configurable logging root namespace in `Loggers`.
- New APIs: `setRoot(String)`, `getRoot()`, and `resetRoot()`.
- Per-mod/per-class explicit APIs: `sdk(String)`, `forMod(String)`, `forClass(String, Class<?>)`, and `forModClass(String, Class<?>)`.

### Changed
- `sdk()` and `forClass(Class<?>)` continue to work with global fallback root.
- Multi-mod usage is now supported via explicit-root APIs so roots like `A/...` and `B/...` can coexist.
- Logging root defaults to `DEFAULT_ROOT` and safely falls back when blank/null is provided.

## [1.0.0] - 2026-03-22

### Added
- Initial public release of NilLoaderSDK.
- Core SDK base for NilLoader mods via `NilModBase`.
- Entrypoint framework with phase dispatching (`premain`, `hijack`) and recursion safeguards.
- Helper utilities for reflection, Minecraft internals, packet handling, and proxy behaviors.
- Utility classes including target resolution and cooldown tracking.
- Java NIO networking stack with client/server, codec, packet registry, and bridge utilities.
- Remapping support through `SimpleRemap`.
- Standardized logging API through `Loggers`.
- Example modules and NilLoader metadata resources.
- Maven publishing configuration and source/javadoc artifacts.

### Build/Tooling
- Gradle-based build with Java toolchain (Java 8).
- NilGradle integration for NilLoader workflows.
- Shadow plugin configured for packaging.
- Optional decompilation task for NilLoader dependency inspection.

### Notes
- This is the first changelog entry for the project.
