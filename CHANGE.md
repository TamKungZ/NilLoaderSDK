# CHANGE

All notable changes to this project will be documented in this file.

## [Unreleased]

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
