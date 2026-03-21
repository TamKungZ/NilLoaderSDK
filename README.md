# NilLoaderSDK

NilLoaderSDK is a utility SDK for NilLoader-based Minecraft mods. It bundles reflection helpers, remapping tools, entrypoint dispatching, a Java NIO networking layer, and standardized logging.

---

## Project Layout (File-by-File)

### Root
- `build.gradle` — build and dependencies
- `settings.gradle` — Gradle settings
- `CHANGE.md` — project change history
- `nilloader.nilmod.css` — NilLoader metadata
- `nilplugin.txt`, `nilplugin-verbose.txt`, `nilagent.txt` — runtime logs

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

### Utilities
- `src/main/java/me/tamkungz/nilloadersdk/util/TargetFinder.java`
- `src/main/java/me/tamkungz/nilloadersdk/util/CooldownTracker.java`

### Remapping
- `src/main/java/me/tamkungz/remapping/SimpleRemap.java`

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

### Example Mods
- `src/main/java/me/tamkungz/nilloadersdk/NilLoaderSDKMod_SDK_Example.java`
- `src/main/java/me/tamkungz/nilloadersdk/LetYourFriendEatingMod_SDK_Example.java`
- `src/main/resources/nilloadersdk.nilmod.css`
- `src/main/resources/letyourfriendeating.nilmod.css`

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
-Dnilloadersdk.network.autoclient.host=127.0.0.1
-Dnilloadersdk.network.autoclient.port=25566
```

Optional:
```text
-Dnilloadersdk.network.autoclient.pollMs=1000
-Dnilloadersdk.network.autoclient.maxFrame=1048576
```

---

## Build

```bat
gradlew.bat compileJava
```

---

## Notes

This document is intentionally updated to match the current, real project state after major logging and entrypoint stability changes.

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

## Maven Publishing

The Gradle build is configured with `maven-publish` in `build.gradle`.

### Publish to local Maven cache

```bat
gradlew.bat publishToMavenLocal
```

### Public package pages

- Package index (direct artifact path):
  - `https://repo.tamkungz.me/me/tamkungz/nilloadersdk/nilloadersdk/1.0.0/`
- Repository web UI (browse/search):
  - `https://repo.tamkungz.me`

### Use in another project

```gradle
repositories {
  maven {
    url "https://repo.tamkungz.me"
  }
}

dependencies {
  implementation "me.tamkungz.nilloadersdk:nilloadersdk:1.0.0"
}
```

### Publish to a remote Maven repository

You can configure publishing via Gradle properties or environment variables.

#### Option A: Gradle properties

```text
-PmavenRepoUrl=https://your.repo/repository/maven-releases/
-PmavenRepoUser=your-username
-PmavenRepoPassword=your-password
```

#### Option B: Environment variables

```text
MAVEN_REPO_URL
MAVEN_REPO_USER
MAVEN_REPO_PASSWORD
```

Then run:

```bat
gradlew.bat publish
```
