<h1>
  <img src="https://raw.githubusercontent.com/NilLoaderSDK/NilLoaderSDK/refs/heads/main/src/main/resources/assets/nilloadersdk/icon.svg"
       width="30"
       alt=""
       aria-hidden="true">
  NilLoaderSDK
</h1>

NilLoaderSDK is a Java 8 utility SDK for NilLoader-based Minecraft mods. Version 3.0.2 keeps the hard runtime link to Minecraft 1.4.7: the core SDK can load across versions, while Minecraft-specific helpers remain mapping/structure dependent.

It provides:
- metadata helpers for NilLoader + KDL
- runtime bootstrap for KDL-only mods
- reflection/remapping helpers
- event bus and lifecycle events
- NIO networking utilities

---

## Why NilLoaderSDK?

NilLoaderSDK is not only metadata tooling. It is a practical utility layer for legacy NilLoader mod development, with reusable APIs that reduce boilerplate and speed up feature development.

Use it when you want a single toolkit for lifecycle dispatching, reflection/remapping, networking, event handling, and modernized metadata support.

---

## Key Features

- **Lifecycle + Entrypoint system**
  - `premain` / `hijack` dispatching via ServiceLoader, JVM properties, and properties file
  - Default SDK modules for centralized startup flow
- **Event bus for mod architecture**
  - Lightweight event system with cancellable events and priority ordering
  - Lifecycle events for pre/post entrypoint dispatch hooks
- **Networking stack (Java NIO)**
  - Client/server implementations with packet registry and codec
  - Optional auto-network bridge for fast integration; version-aware and fail-closed when mappings are unavailable
- **Reflection + remapping helpers**
  - Utilities for interacting with obfuscated legacy internals safely and repeatedly
  - Descriptor-aware SRG/CSRG inspect, reverse, chain, and lookup tooling
  - Pinned MinecraftRemapping Git submodule used directly as build input; complete mapping files are not copied into releases
- **Metadata bridge (CSS + KDL)**
  - Supports `.nilmod.css` and `.nilsdkmod.kdl`
  - KDL-only runtime bootstrap for SDK-aware mods when root CSS is absent
  - Dependency policy support (`requires`, `safeload`, load order hints)
- **Developer toolbox shadow JAR**
  - Optional Byte Buddy, GEB, ClassGraph, and SnakeYAML APIs in `-all.jar` without package relocation
  - Helper facades for instrumentation, scanning, YAML, and GEB bootstrap
- **Diagnostics for pack/mod developers**
  - Verbose bootstrap pipeline logs
  - Loaded-mod table output:
    - `ID | Name | Version | Authors | License`

---

## Metadata Example (`.nilsdkmod.kdl`)

```kdl
nilmod {
  name "My Mod"
  description "Example mod"
  authors "Author"
  version "1.0.0"
}

entrypoints {
  premain "com.example.MyPremain"
  hijack "com.example.MyHijack"
}

nilloadersdk {
  requires "nilloader" "nilloadersdk"
  load_after "nilloadersdk"
  icon "assets/mymod/icon.png"
  modurl "https://modrinth.com/mod/my-mod"
  sourceurl "https://github.com/example/my-mod"
  license "MIT"
  credits "Author"
}
```

---

## Dependency (Gradle)

For `3.0.2`, publish the SDK into its project-local `./maven` repository first:

```bash
./gradlew publishProjectLocal
```

Then point the consuming project at that directory:

```gradle
repositories {
  maven { url = uri("https://repo.tamkungz.me") }
}

dependencies {
  implementation "me.tamkungz.nilloadersdk:nilloadersdk:3.0.2"
}
```

The project-local publication is GPG-signed.

---

## Keywords (for search)

NilLoader, NilLoaderSDK, Minecraft 1.4.7, legacy Minecraft modding, Java 8 modding, entrypoint framework, event bus, NIO networking, reflection helper, remapping tools, KDL metadata, nilmod SDK.

---

## License

Licensed under **LGPL-3.0-or-later**.

---

## Credits

- [NilLoader (official)](https://git.sleeping.town/Nil/NilLoader)
- [NilLoader (mirror)](https://github.com/exaskye/NilLoader)
