# CHANGE

All notable changes to this project will be documented in this file.

## [3.0.0] - 2026-08-05

### Added
- Added `agaricusb/MinecraftRemapping` as an external Git submodule configuration at `tools/MinecraftRemapping`, pinned by the bootstrap scripts to `8ca7ba25dfd67eae43b3c73d02603ff6c085a6d7`.
- Added a Java 8-compatible SRG/CSRG mapping API: `SrgMappingSet` and `SrgMappings`.
- Added `MappingToolMain` with `inspect`, `reverse`, `chain`, `lookup`, `list-submodule`, and `import-submodule` commands, plus a standalone `mapping-tool` JAR that contains no mapping data.
- Added Gradle `mappingTool` and `prepareRemapping` tasks for local mapping workflows.
- Added `MAPPINGS.md` documenting mapping-source policy and local-only import behavior.
- Added cross-platform Gradle launcher JVM discovery for Linux/macOS and Windows. JDK 21 and 17 are preferred automatically so a system-wide Java 25 does not break Gradle 8.8.
- Added GitHub Actions `build.yml` for push/pull-request builds and tests on Ubuntu/Windows with JDK 17/21.
- Added GitHub Actions `release.yml` for `v*` tags. Releases are created only after build/tests succeed, include JARs plus SHA-256 checksums, and use the matching `CHANGE.md` section as the release body.
- Added release helper scripts that verify tag/project version consistency and extract one changelog section.

### Changed
- Bumped the SDK and metadata version to `3.0.0`.
- Java compilation now targets Java 8 with `--release 8` while Gradle itself runs on a supported modern launcher JDK; a dedicated local JDK 8 installation is no longer required for normal builds.
- Complete mapping collections are no longer shipped in the project ZIP/release tree. `.remapping/` remains gitignored and is treated as developer-supplied local build input.
- GitHub release responsibilities were separated from normal commit CI: `build.yml` only validates commits/PRs, while `release.yml` owns tagged releases.

### Fixed
- Fixed Gradle 8.8 startup failing under Java 25 with `Unsupported class file major version 69` by selecting a compatible installed launcher JDK before Gradle starts.
- Fixed old mapping workflows relying on copied 100+ MB mapping trees by replacing them with explicit local import tooling and an external submodule reference.
- Mapping parsing now reports conflicting entries instead of silently replacing them, mapping composition keeps method descriptors namespace-correct, and CSRG member resolution no longer depends on class-entry order.

## [2.1.0] - 2026-08-05

### Added
- Added project-local Maven publishing to `./maven` via `publishProjectLocal`; publications are OpenPGP-signed with the user's normal GnuPG / `gpg-agent` setup.
- Added inherited event-listener discovery, typed-listener unregistration, and event-bus clearing APIs.
- Added cooldown remaining-time queries and expired-player cleanup.
- Added NIO client/server state inspection (`isRunning`, `isConnected`, connection snapshots/counts) and server bound-port lookup.
- Added default listener hooks for unknown packet IDs and exhausted client reconnect attempts without breaking existing listener implementations.
- Added packet-registry inspection helpers (`isRegistered`, `size`, `clear`).
- Added lifecycle controls/state for the automatic Minecraft network bridge, including explicit `stop()` and automatic recovery after reconnect exhaustion.
- Added regression tests for KDL, cooldown concurrency, and packet codec/registry behavior.

### Changed
- Normal `build` no longer forces NilLoader decompilation; `decompileNilloader` remains an explicit developer task.
- KDL scalar output now uses KDL 2 forms (`#true`, `#false`, `#null`, `#inf`, `#-inf`, `#nan`).
- KDL parsing now accepts common KDL 2 numeric forms, raw strings, quoted property keys, dotted identifiers, nested block comments, and slash-dash node/entry suppression while retaining legacy boolean/null compatibility.
- Event registration is de-duplicated, scans inherited subscriber methods, and preserves deterministic registration order for equal-priority handlers.
- Network listener callback failures are isolated from the selector loop; packet serialization failures no longer disconnect healthy peers.
- Network constructor and packet-registry inputs are validated early with clearer errors.

### Fixed
- Fixed `TargetFinder` returning a nearby player that failed the requested `minDot` aim threshold and fixed target ranking to prefer alignment with distance as the tie-breaker.
- Fixed `CooldownTracker.tryUseGlobal` / `tryUsePlayer` race conditions so a cooldown window cannot be consumed concurrently by multiple callers.
- Fixed entrypoint phase re-entrancy during the pre-dispatch event by installing the active-phase guard before posting the event.
- Fixed NIO server duplicate disconnect callbacks and stale connection entries.
- Fixed NIO client reconnect attempts terminating permanently when opening a replacement channel throws immediately or when a non-blocking connect completes immediately.
- Fixed KDL writer/parser round-trip failures for quoted property keys and KDL 2 scalar values.
- Fixed zero/invalid look-vector and negative-range edge cases in `TargetFinder`.
- Fixed `ReflectHelper.invoke` contradicting its documented null-to-primitive-default behavior.
- Fixed automatic network bridge getting stuck with a dead client after reconnect exhaustion.
- Fixed local SRG mapping reads using platform-default encoding and added clearer version validation.

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
