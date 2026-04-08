# CHANGE

All notable changes to this project will be documented in this file.

## [2.0.1] - 2026-04-08

### Added
- Added SDK runtime bootstrap for KDL-only mods (archives with `.nilsdkmod.kdl` but no root `*.nilmod.css`).
- Added `KdlOnlyModBootstrapper` to discover/inject such mods from `mods/` and `nilmods/` during SDK premain.
- Added verbose bootstrap diagnostics for KDL-only loading flow (candidate scan, KDL entry detection, metadata parse status, classpath injection, premain invocation).
- Added formatted loaded-mod summary table log:
  - `ID | Name | Version | Authors | License`

### Changed
- Bumped SDK version to `2.0.1` in build and metadata resources.
- `DefaultSdkEntrypointModule` now runs KDL-only bootstrap before dependency enforcement.

### Fixed
- Fixed KDL parser newline/block handling in nested sections, resolving `KdlParseException` during valid `.nilsdkmod.kdl` parsing.
- Improved metadata text decoding for runtime reads (UTF-8/UTF-16 BOM and UTF-16 heuristic fallback) in bridge and SDK metadata IO.
- Fixed KDL-only metadata resolution so injected mods now correctly expose `name`, `version`, and `entrypoints` instead of fallback `?` values.

## [2.0.0] - 2026-04-08

### Added
- Introduced KDL support as a first-class metadata layer in `2.0.0`.
  - Prior to `2.0.0`, mods relied on NilLoader CSS metadata only (`*.nilmod.css`).
  - `2.0.0` adds SDK KDL metadata (`*.nilsdkmod.kdl`) and runtime bridge integration.
- Introduced a general-purpose KDL toolkit for broad SDK usage (not metadata-only):
  - `KdlParser`, `KdlWriter`
  - `KdlDocument`, `KdlNode`, `KdlValue`, `KdlParseException`
  - Can be used by any system/module that needs KDL parsing/serialization.
- `NilLoaderHelper` convenience APIs for easier multi-mod integration and diagnostics:
  - `isAllModsLoaded(String...)`
  - `getFirstLoadedMod(String...)`
  - `getEntrypoints(String)`
  - `hasEntrypoint(String, String)`
  - `getModsWithEntrypoint(String)`
  - `hasMissingRequiredMods(String)`
  - `getMissingRequiredModsForLoadedMods()`
  - `getModsRequiring(String)`
- Forge/Fabric-like event architecture for easier mod development:
  - Global SDK access point: `NilLoaderSDK`
  - Event primitives: `Event`, `CancellableEvent`, `EventPriority`, `SubscribeEvent`
  - Central `EventBus` with:
    - annotation listener registration (`@SubscribeEvent`)
    - typed callback registration (`listen` style)
    - cancellation-aware dispatch flow
  - Lifecycle events:
    - `PreEntrypointDispatchEvent` (cancellable)
    - `PhaseEvent`
    - `PostEntrypointDispatchEvent`
  - Entrypoint dispatcher now emits lifecycle events around phase execution.
  - `NilModBase` now includes convenience methods to register/post/listen events.
- SDK KDL metadata schema expanded with richer mod info fields:
  - `modurl`
  - `sourceurl`
  - `license`
  - `credits` (multi-value)

### Changed
- Bumped SDK version to `2.0.0` in build and metadata resources.
- `NilMetadataBridge` now parses `.nilsdkmod.kdl` via shared KDL parser (`KdlParser`) instead of manual string parsing for better compatibility.
- KDL metadata merge now supports both section blocks (`nilmod {}`, `entrypoints {}`) and top-level fallback keys (`name`, `description`, `authors`, `version`, `entrypoints.<phase>`).

### Fixed
- Removed inconsistent changelog carry-over for `1.0.4` under `2.0.0`.

### Notes
- New helper methods are Java 8 compatible and return immutable collections where applicable.
- Focus of this update is DX (developer experience): reduce repetitive NilLoader metadata and dependency-check boilerplate in mods.
- Backward compatibility remains intact: CSS metadata stays primary, and KDL is additive for SDK-aware features.

## [1.0.3] - 2026-03-26

### Changed
- Bumped SDK version to `1.0.3` in build and metadata resources.
- KDL metadata parser remains custom/in-project (Java 8 compatible), without external KDL dependency.

### Added
- SDK-only metadata model and IO:
  - `SdkModMetadata`
  - `SdkMetadataKdl`
  - `SdkMetadataIO`
- Runtime metadata bridge in SDK:
  - `NilMetadataBridge`
  - `NilMetadataPatchInstaller`
  - Patches `NilMetadata.from` during premain to merge CSS + KDL automatically.
- New SDK metadata resource source-of-truth: `src/main/resources/nilloadersdk.nilsdkmod.kdl`.
- `NilLoaderHelper` SDK metadata APIs:
  - `getSdkMetadata(String)` / `getSdkMetadata(NilMetadata)`
  - `getMissingRequiredMods(String)`
  - `areRequiredModsLoaded(String)`
  - `getLoadBefore(String)`
  - `getLoadAfter(String)`
  - `getIconPath(String)`
  - `getLoadedModIcons()`
  - `getRequiredMods(String)`
  - `isSafeLoad(String)`

### Notes
- SDK-only metadata is separated from NilLoader base metadata for compatibility.
- SDK metadata default file is now `.nilsdkmod.kdl` (legacy `.kdl` names are still readable).
- Merge policy: CSS is primary; KDL only fills missing metadata fields.
- No per-mod custom Gradle metadata-generation step required.
- Dependency enforcement:
  - Missing required mods + `safeload=true` -> warn log
  - Missing required mods + `safeload=false` -> error and stop startup
- If SDK is not installed, NilLoader still reads only original `*.nilmod.css` and continues to work normally.

## [1.0.2] - 2026-03-24

### Added
- New helper: `NilLoaderHelper` in `me.tamkungz.nilloadersdk.helper`.
- Convenience APIs for NilLoader metadata and loaded-mod checks:
  - `isModLoaded`, `isAnyModLoaded`
  - `getModMetadata`, `getModMetadataOrNull`, `getAllLoadedMods`
  - `getLoadedModIds`, `getLoadedModNames`, `getLoadedModsById`
  - `getSourceFile`, `getEntrypointNames`, `getEntrypointClass`, `describeMod`
- New helper: `TransformerHelper` in `me.tamkungz.nilloadersdk.helper`.
- Java-agent style class patch registration via NilLoader transformer pipeline (no Mixin required):
  - `registerBytecodePatch` for raw byte[] transforms
  - `registerAsmPatch` for ASM `ClassNode` transforms
  - class-name normalization utilities for internal slash format

### Changed
- `SimpleRemap.forVersion("1.4.7")` preserves manual mappings from `build147()` as higher priority.
- External SRG (`.remapping/1.4.7/mcp2obf.srg`) is used only to fill missing entries, not overwrite existing `build147()` mappings.
- `SimpleRemap.forVersion(version)` can load remap for versions that provide local `.remapping/<version>/mcp2obf.srg`.

### Packaging
- SRG files are not bundled into the built JAR.
- Build now auto-generates `GeneratedSrgMappings` from local `.remapping/*/mcp2obf.srg` and embeds only the extracted mappings used by SDK remap calls.
- Runtime loads generated mappings first via `SimpleRemap`, then keeps fallback behavior for local development.

### Notes
- `.remapping` is not bundled in the repository contents.
- If you want to build and use remapping locally, prepare/provide your own `.remapping` directory.

### Docs
- README helper section now includes `NilLoaderHelper` and summarizes key API groups.
- README now documents class patching usage through `TransformerHelper` and phase timing notes (`premain` / `hijack`).

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
