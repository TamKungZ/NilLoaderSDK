# CHANGE

All notable changes to this project will be documented in this file.

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
