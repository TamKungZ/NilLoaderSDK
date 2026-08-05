# NilLoaderSDK

NilLoaderSDK is a utility SDK for NilLoader-based Minecraft mods. It bundles reflection helpers, remapping tools, entrypoint dispatching, a Java NIO networking layer, and standardized logging.

---

## Minecraft compatibility

NilLoaderSDK `3.0.2` no longer has a hard bytecode dependency on a specific Minecraft JAR. Core systems such as events, KDL, metadata helpers, networking, logging, reflection, and mapping tooling can load independently of Minecraft 1.4.7.

Minecraft-facing helpers are lazy and reflection-based. They only attempt to resolve Minecraft classes when explicitly called, and they require mappings/structure compatible with the running game version. Minecraft 1.4.7 still has built-in fallback mappings; additional mapping subsets are generated at build time from the pinned `tools/MinecraftRemapping` submodule.

The optional auto-network bridge is disabled by default. If enabled, also provide the target version, for example:

```text
-Dnilloadersdk.network.autoclient.enabled=true
-Dnilloadersdk.minecraft.version=1.4.7
```

If the version or mapping is unavailable, the bridge disables itself instead of failing game startup.

---

## Project Layout (File-by-File)

### Root
- `build.gradle` — build and dependencies
- `settings.gradle` — Gradle settings
- `CHANGE.md` — project change history
- `nilloader.nilmod.css` — NilLoader metadata
- `nilplugin.txt`, `nilplugin-verbose.txt`, `nilagent.txt` — checked-in decompiler/reference dumps used during SDK development

### Core SDK
- `src/main/java/me/tamkungz/nilloadersdk/NilModBase.java` — mod base class
- `src/main/java/me/tamkungz/nilloadersdk/build/ModBuildInfo.java` — build metadata

### Logging
- `src/main/java/me/tamkungz/nilloadersdk/log/Loggers.java`
  - Root logger: `sdk()`
  - Class logger: `forClass(Class<?>)`
  - Configurable root namespace: `setRoot(String)`, `getRoot()`, `resetRoot()`
  - Default root namespace: `DEFAULT_ROOT` (`NilLoaderSDK`)

### Entrypoint System
- `src/main/java/me/tamkungz/nilloadersdk/entrypoint/NilLoaderSDKPremain.java`
- `src/main/java/me/tamkungz/nilloadersdk/entrypoint/NilLoaderSDKHijack.java`
- `src/main/java/me/tamkungz/nilloadersdk/entrypoint/EntrypointDispatcher.java`
  - Dispatches phases (`premain`, `hijack`)
  - Supports ServiceLoader + JVM properties + properties file
  - Prevents re-entrant phase dispatch
  - Skips self-target recursion
- `src/main/java/me/tamkungz/nilloadersdk/entrypoint/NilLoaderSDKEntrypointModule.java`
- `src/main/java/me/tamkungz/nilloadersdk/entrypoint/DefaultSdkEntrypointModule.java`
- `src/main/resources/nilloadersdk.entrypoints.properties`
  - Default safe values:
    - `premain=`
    - `hijack=`
- `src/main/resources/META-INF/services/me.tamkungz.nilloadersdk.entrypoint.NilLoaderSDKEntrypointModule`

### Helpers
- `src/main/java/me/tamkungz/nilloadersdk/helper/ReflectHelper.java`
- `src/main/java/me/tamkungz/nilloadersdk/helper/McHelper.java`
- `src/main/java/me/tamkungz/nilloadersdk/helper/PacketHelper.java`
- `src/main/java/me/tamkungz/nilloadersdk/helper/ProxyHelper.java`
- `src/main/java/me/tamkungz/nilloadersdk/helper/NilLoaderHelper.java`
  - Convenience APIs for loaded mod lookup (`isModLoaded`, `getAllLoadedMods`)
  - Group checks and fallback lookup (`isAllModsLoaded`, `getFirstLoadedMod`)
  - Metadata extraction (`getLoadedModIds`, `getLoadedModNames`, `describeMod`)
  - Entrypoint metadata utilities (`getEntrypointNames`, `getEntrypointClass`, `getEntrypoints`, `hasEntrypoint`, `getModsWithEntrypoint`)
  - SDK-only metadata utilities (`getSdkMetadata`, `getMissingRequiredMods`, `getLoadBefore`, `getLoadAfter`, `getIconPath`)
  - Dependency diagnostics (`hasMissingRequiredMods`, `getMissingRequiredModsForLoadedMods`, `getModsRequiring`)
- `src/main/java/me/tamkungz/nilloadersdk/helper/TransformerHelper.java`
  - Register class patchers without Mixin using NilLoader transformers
  - Supports raw bytecode patch callback and ASM `ClassNode` patch callback
  - Useful for Java-agent style class overwrite/edit during `premain` / `hijack`

### Event System (Forge/Fabric-like)
- `src/main/java/me/tamkungz/nilloadersdk/NilLoaderSDK.java`
  - Global access for event registration and dispatch
- `src/main/java/me/tamkungz/nilloadersdk/event/EventBus.java`
  - Annotation listener registration (`@SubscribeEvent`)
  - Typed listener callback registration
  - Priority ordering (`HIGHEST -> LOWEST`)
  - Cancellation-aware dispatch
- `src/main/java/me/tamkungz/nilloadersdk/event/Event.java`
- `src/main/java/me/tamkungz/nilloadersdk/event/CancellableEvent.java`
- `src/main/java/me/tamkungz/nilloadersdk/event/SubscribeEvent.java`
- `src/main/java/me/tamkungz/nilloadersdk/event/EventPriority.java`
- Lifecycle hook events:
  - `PreEntrypointDispatchEvent` (cancellable)
  - `PhaseEvent`
  - `PostEntrypointDispatchEvent`

Quick usage:

```java
public final class MyModEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPhase(PhaseEvent e) {
        // receives premain/hijack phase notifications
    }
}

// register annotation listeners
NilLoaderSDK.registerEvents(new MyModEvents());

// register typed callback listeners
NilLoaderSDK.listen(PhaseEvent.class, e -> {
    // handle phase event
});
```

Inside `NilModBase`, convenience methods are available:
- `registerEvents()` / `registerEvents(Object)`
- `listen(Class<T>, EventListener<T>)` / `unlisten(Class<T>, EventListener<T>)`
- `post(Event)`

### Developer Toolbox (`-all.jar`)

`NilLoaderSDK-3.0.2-all.jar` keeps the normal SDK API and additionally bundles optional developer libraries **without relocation** so downstream mods can import their upstream packages directly. The normal Maven artifact does not publish these libraries as transitive runtime dependencies.

Bundled runtime tooling:
- Byte Buddy `1.17.6` + Byte Buddy Agent `1.17.6` — runtime code generation/instrumentation
- GEB core `0.5.4` — optional generated event bus
- ClassGraph `4.8.184` — classpath/annotation discovery
- SnakeYAML `2.6` — YAML parsing/emitting

Convenience facades live under `me.tamkungz.nilloadersdk.tooling`:

```java
Map<String, Boolean> available = DeveloperToolbox.availability();
Map<String, Object> yaml = YamlHelper.loadMap("enabled: true\n");
List<String> listeners = ClassGraphHelper.classesWithAnnotation(
        "com.example.AutoRegister", "com.example"
);
Class<? extends MyBase> generated = ByteBuddyHelper.makeSubclass(MyBase.class);
GEB bus = GebHelper.createBus();
```

Byte Buddy self-attachment is never performed during SDK startup; `ByteBuddyHelper.installInstrumentation()` is explicit and may still be unavailable on JVMs/platforms that restrict attach (or when upstream agent self-attachment cannot operate from a repackaged JAR). Prefer instrumentation already supplied by the loader/agent when possible. YAML loading uses SnakeYAML's safe constructor and rejects duplicate keys.

The helper classes themselves are present in the normal SDK for a stable API surface, but helpers that expose Byte Buddy/GEB/ClassGraph/SnakeYAML types require either the `-all.jar` or the matching upstream dependency on the consuming project's classpath. `DeveloperToolbox` itself has no hard optional-library dependency and is safe to use with the normal JAR.

GEB's annotation processor is build-time only and is deliberately **not** put into the shadow JAR. A downstream project that authors GEB `@Listen` handlers should add:

```gradle
annotationProcessor 'foo.zaaarf.geb:processor:0.4.9'
```

Use the normal JAR if you do not need this toolbox; use `-all.jar` when you want these APIs available without maintaining the additional runtime dependency list.

### SDK-only Metadata (.nilsdkmod.kdl)
- `src/main/java/me/tamkungz/nilloadersdk/metadata/SdkModMetadata.java`
- `src/main/java/me/tamkungz/nilloadersdk/metadata/SdkMetadataKdl.java`
- `src/main/java/me/tamkungz/nilloadersdk/metadata/SdkMetadataIO.java`
- `src/main/java/me/tamkungz/nilloadersdk/metadata/NilMetadataBridge.java`
- `src/main/java/me/tamkungz/nilloadersdk/metadata/NilMetadataPatchInstaller.java`
- `src/main/java/me/tamkungz/nilloadersdk/metadata/KdlOnlyModBootstrapper.java`
- `src/main/resources/nilloadersdk.nilsdkmod.kdl`

### General-purpose KDL Toolkit (SDK-wide)
- `src/main/java/me/tamkungz/nilloadersdk/util/kdl/KdlParser.java`
- `src/main/java/me/tamkungz/nilloadersdk/util/kdl/KdlWriter.java`
- `src/main/java/me/tamkungz/nilloadersdk/util/kdl/KdlDocument.java`
- `src/main/java/me/tamkungz/nilloadersdk/util/kdl/KdlNode.java`
- `src/main/java/me/tamkungz/nilloadersdk/util/kdl/KdlValue.java`
- `src/main/java/me/tamkungz/nilloadersdk/util/kdl/KdlParseException.java`

These classes are reusable KDL utilities for any SDK module, not only metadata.

This metadata is **SDK-only** and kept separate from NilLoader base metadata to preserve compatibility.

Version note:
- SDK KDL metadata support is introduced as part of `2.0.0` (before that, metadata was CSS-only in `*.nilmod.css`).
- `2.0.1` adds SDK-side runtime bootstrap for KDL-only mods (mods that ship `.nilsdkmod.kdl` without root `*.nilmod.css`).
- `2.1.0` focuses on reliability: KDL 2 syntax coverage/round-tripping, safer event dispatch, atomic cooldowns, stricter targeting, and hardened NIO reconnect/disconnect handling.
- `3.0.0` adds descriptor-aware SRG tooling, the external mapping submodule, cross-platform Gradle JVM discovery, and separated commit/release CI.
- `3.0.1` removes the hard Minecraft 1.4.7 class link, makes Minecraft access lazy/reflection-only, and reads mapping input directly from `tools/MinecraftRemapping`.
- `3.0.2` adds the optional developer-toolbox shadow JAR, safe helper facades for Byte Buddy/ClassGraph/SnakeYAML/GEB, corrected third-party licensing, and restored cross-platform CI/release automation.

Compatibility/runtime behavior:
- NilLoader (without this SDK) ignores it.
- SDK patches NilLoader metadata creation at runtime (`premain`) so users do not need custom Gradle metadata-generation steps.
- SDK can bootstrap KDL-only mods at runtime (mods that ship `.nilsdkmod.kdl` without root `*.nilmod.css`) after SDK premain is reached.
- Merge policy when both files exist: CSS is primary, missing fields are filled from KDL.
- KDL parsing in the runtime bridge now uses the shared in-project parser (`KdlParser`) instead of manual string parsing.
- Metadata extraction supports both section blocks (`nilmod {}`, `entrypoints {}`) and top-level fallback keys (`name`, `description`, `authors`, `version`, `entrypoints.<phase>`).
- SDK-aware mods can declare:
  - Required mod IDs
  - Advisory load ordering (`load_before`, `load_after`)
  - Icon path
  - Mod URL (`modurl`)
  - Source URL (`sourceurl`)
  - License (`license`)
  - Shared credits (`credits`)
  - `safeload` (`true` default; when `false`, missing required mods cause hard error)

Runtime dependency policy (SDK built-in):
- Missing required mods + `safeload=true` => warn in SDK logger.
- Missing required mods + `safeload=false` => error and stop startup.

Bootstrap observability/logging:
- KDL-only bootstrap emits step-by-step diagnostics (candidate scan, metadata entry detection, parse status, inject status).
- SDK prints a compact loaded-mod table for quick diagnostics:
  - `ID | Name | Version | Authors | License`

Easy APIs for UI/modmenu usage:
- `NilLoaderHelper.getIconPath(id)`
- `NilLoaderHelper.getLoadedModIcons()`
- `NilLoaderHelper.getRequiredMods(id)`
- `NilLoaderHelper.isSafeLoad(id)`
- `NilLoaderHelper.getModUrl(id)`
- `NilLoaderHelper.getSourceUrl(id)`
- `NilLoaderHelper.getLicense(id)`
- `NilLoaderHelper.getCredits(id)`

Convenience dependency/introspection APIs:
- `NilLoaderHelper.isAllModsLoaded("a", "b")`
- `NilLoaderHelper.getFirstLoadedMod("fabricproxy", "legacyproxy")`
- `NilLoaderHelper.getModsWithEntrypoint("hijack")`
- `NilLoaderHelper.getMissingRequiredModsForLoadedMods()`

Example KDL:

```kdl
nilloadersdk {
  requires "nilloader" "other_core_mod"
  load_before "optional_patch_mod"
  load_after "library_mod"
  icon "assets/example/icon.png"
  modurl "https://example.com/mod"
  sourceurl "https://github.com/example/mod"
  license "MIT"
  credits "Alice" "Bob"
}
```

Combined source file example (`.nilsdkmod.kdl`):

```kdl
nilmod {
  name "MyMod"
  description "My mod"
  authors "Author"
  version "1.0.0"
}

entrypoints {
  premain "com.example.MyPremain"
  hijack "com.example.MyHijack"
}

nilloadersdk {
  requires "nilloader" "other_core_mod"
  load_before "optional_patch_mod"
  load_after "library_mod"
  icon "assets/example/icon.png"
}
```

### Class Patching (No Mixin)

If your environment cannot use Mixin, you can patch classes via NilLoader transformer APIs:

```java
TransformerHelper.registerAsmPatch("net.minecraft.client.Minecraft", (loader, cn) -> {
    // modify cn.methods / instructions here
    return true; // true when frame recomputation is needed
});
```

Or use raw byte arrays:

```java
TransformerHelper.registerBytecodePatch("net.minecraft.client.Minecraft", (loader, name, bytes) -> {
    // return modified class bytes (or null to keep original)
    return bytes;
});
```

Important: register patches in SDK entrypoint phases (`premain` / `hijack`) before transformer registration is frozen.

### Utilities
- `src/main/java/me/tamkungz/nilloadersdk/util/TargetFinder.java`
- `src/main/java/me/tamkungz/nilloadersdk/util/CooldownTracker.java`

### Remapping and Mapping Tooling
- `src/main/java/me/tamkungz/remapping/SimpleRemap.java` — compatibility-oriented runtime lookup API.
- `src/main/java/me/tamkungz/nilloadersdk/mapping/SrgMappingSet.java` — descriptor-aware SRG mapping model.
- `src/main/java/me/tamkungz/nilloadersdk/mapping/SrgMappings.java` — read/write/reverse/chain utilities for SRG/CSRG files.
- `src/main/java/me/tamkungz/nilloadersdk/mapping/MappingToolMain.java` — local CLI used by the Gradle `mappingTool` task and standalone mapping-tool JAR.
- `tools/MinecraftRemapping` — external `agaricusb/MinecraftRemapping` Git submodule.
- `MAPPINGS.md` — mapping-source and redistribution policy.

NilLoaderSDK does not vendor a second mapping copy. Mapping input is read directly from the pinned `tools/MinecraftRemapping` submodule.

Examples:

```bash
./gradlew mappingTool --args="inspect tools/MinecraftRemapping/1.4.7/mcp2obf.srg"
./gradlew mappingTool --args="reverse input.srg output.srg"
./gradlew mappingTool --args="chain first.srg second.srg output.srg"
./gradlew inspectMinecraftRemapping -PmcVersion=1.4.7
./gradlew mappingToolJar
```

The standalone JAR is emitted as `build/libs/nilloadersdk-3.0.2-mapping-tool.jar` and contains only the mapping utility code, never mapping data. It can be used without launching Minecraft:

```bash
java -jar build/libs/nilloadersdk-3.0.2-mapping-tool.jar inspect input.srg
```

See [`MAPPINGS.md`](MAPPINGS.md) for submodule setup and licensing notes.

### Networking (Java NIO)
- `src/main/java/me/tamkungz/nilloadersdk/network/Connection.java`
- `src/main/java/me/tamkungz/nilloadersdk/network/NioServer.java`
- `src/main/java/me/tamkungz/nilloadersdk/network/NioClient.java`
- `src/main/java/me/tamkungz/nilloadersdk/network/ServerListener.java`
- `src/main/java/me/tamkungz/nilloadersdk/network/ClientListener.java`
- `src/main/java/me/tamkungz/nilloadersdk/network/MinecraftAutoNetworkBridge.java`
- `src/main/java/me/tamkungz/nilloadersdk/network/codec/PacketCodec.java`
- `src/main/java/me/tamkungz/nilloadersdk/network/packet/Packet.java`
- `src/main/java/me/tamkungz/nilloadersdk/network/packet/PacketFactory.java`
- `src/main/java/me/tamkungz/nilloadersdk/network/packet/PacketRegistry.java`

### SDK Metadata Resources
- `src/main/resources/nilloadersdk.nilmod.css`
- `src/main/resources/nilloadersdk.nilsdkmod.kdl`

---

## Logging Standard

### Rules
1. For single-mod apps, you can use default root: `NilLoaderSDK`.
2. For multi-mod apps, use explicit per-mod roots (example: `A`, `B`).
3. Use class-scoped logger when needed: `<ModRoot>/<ClassSimpleName>`.
4. Do not hardcode bracket prefixes (`[]`) in log message text.

### Example (Per-Mod)
```java
private static final NilLogger A_LOG = Loggers.forMod("A");
private static final NilLogger A_CLASS_LOG = Loggers.forModClass("A", ModAService.class);

private static final NilLogger B_LOG = Loggers.forMod("B");
private static final NilLogger B_CLASS_LOG = Loggers.forModClass("B", ModBService.class);

A_LOG.info("A root logger");      // A
A_CLASS_LOG.info("A class logger"); // A/ModAService

B_LOG.info("B root logger");      // B
B_CLASS_LOG.info("B class logger"); // B/ModBService
```

---

## Entrypoint Routing

Entrypoints are resolved in this order:
1. JVM property: `-Dnilloadersdk.entrypoint.<phase>=...`
2. Properties file: `src/main/resources/nilloadersdk.entrypoints.properties`
3. ServiceLoader modules (`NilLoaderSDKEntrypointModule`)

Important: never point `premain` or `hijack` to SDK self-entrypoint classes, or it will recurse.

---

## Auto Network Bridge (JVM Properties)

Enable:
```text
-Dnilloadersdk.network.autoclient.enabled=true
-Dnilloadersdk.minecraft.version=1.4.7
-Dnilloadersdk.network.autoclient.host=127.0.0.1
-Dnilloadersdk.network.autoclient.port=25566
```

The bridge fails closed: if the requested Minecraft version has no available mapping subset, it logs a warning and disables itself instead of aborting startup.

Optional:
```text
-Dnilloadersdk.network.autoclient.pollMs=1000
-Dnilloadersdk.network.autoclient.maxFrame=1048576
```

---

## Build

Normal builds no longer decompile NilLoader as a side effect. Decompilation remains available as an explicit developer task.

The wrapper now chooses a Gradle-compatible installed JDK automatically. JDK **21** is preferred, then **17**; this prevents an ambient Java 25 installation from starting Gradle 8.8 and failing with class-file major version 69. Compilation itself still targets Java 8 via `--release 8`.

Override the launcher JDK when needed:

```bash
NILSDK_GRADLE_JAVA_HOME=/path/to/jdk-21 ./gradlew clean build
```

PowerShell:

```powershell
$env:NILSDK_GRADLE_JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"
.\gradlew.bat clean build
```

Normal build:

```bash
./gradlew clean build
```

Windows:

```bat
gradlew.bat clean build
```

Optional NilLoader decompile:

```bash
./gradlew decompileNilloader
```

### GitHub Actions

- `.github/workflows/build.yml` runs build/tests for every pushed commit and pull request on Ubuntu + Windows using JDK 17 and 21.
- `.github/workflows/release.yml` runs only for tags matching `v*`. It verifies the tag matches `build.gradle`, builds/tests, extracts that version's section from `CHANGE.md`, and creates the GitHub Release with JARs and SHA-256 checksums.

---

## Notes

This document tracks the current 3.x project layout and build/runtime behavior.

---

## Credits

- NilLoader upstream (official): https://git.sleeping.town/Nil/NilLoader
- NilLoader mirror: https://github.com/exaskye/NilLoader

---

## License

This project is licensed under the GNU Lesser General Public License v3.0 or later (LGPL-3.0-or-later).

- License file: `LICENSE`
- License text: https://www.gnu.org/licenses/lgpl-3.0.txt

---

## Maven Publishing (Project-local + GPG signed)

Version `3.0.2` publishes to a Maven repository **inside this project** at `./maven/`; it does not publish to `~/.m2` and no remote repository credentials are required. The publication includes the main JAR, `-all` developer-toolbox JAR, standalone `-mapping-tool` JAR, sources JAR, Javadoc JAR, POM/module metadata, checksums, and OpenPGP `.asc` signatures.

GPG must be installed and available on `PATH`. The build uses Gradle's `useGpgCmd()`, so your normal GnuPG configuration and `gpg-agent` are used; private keys are never stored in this repository. The default GPG key is used unless you configure another key.

Publish:

```bash
./gradlew publishProjectLocal
```

Windows:

```bat
gradlew.bat publishProjectLocal
```

Artifacts are written under:

```text
maven/me/tamkungz/nilloadersdk/nilloadersdk/3.0.2/
```

The classified artifacts are:

```text
nilloadersdk-3.0.2-all.jar          # SDK + optional developer toolbox
nilloadersdk-3.0.2-mapping-tool.jar # standalone SRG/CSRG CLI, no mappings bundled
```

To select a specific GPG key, put the setting in your user Gradle configuration (recommended: `~/.gradle/gradle.properties`) rather than committing secrets to this project:

```properties
signing.gnupg.keyName=YOUR_KEY_ID
```

If your GPG setup needs a passphrase, prefer letting `gpg-agent` prompt/cache it instead of committing a passphrase in `gradle.properties`.

### Use the project-local repository from another Gradle project

Point the consumer at this project's `maven` directory:

```gradle
repositories {
    maven { url = uri('/absolute/path/to/NilLoaderSDK/maven') }
}

dependencies {
    implementation 'me.tamkungz.nilloadersdk:nilloadersdk:3.0.2'
}
```

For a sibling checkout you can use a relative path, for example:

```gradle
repositories {
    maven { url = uri('../NilLoaderSDK/maven') }
}
```

Clean only the generated project-local Maven repository:

```bash
./gradlew cleanProjectLocalMaven
```
