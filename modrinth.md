# NilLoaderSDK

**NilLoaderSDK** is a utility SDK for NilLoader-based Minecraft mods. It bundles reflection helpers, remapping tools, entrypoint dispatching, a Java NIO networking layer, and standardized logging.

---

## Features

- **Standardized Logging** — root & class-scoped loggers under `NilLoaderSDK` namespace
- **Entrypoint Dispatching** — supports `premain` and `hijack` phases via ServiceLoader, JVM properties, or properties file
- **Reflection & Remap Helpers** — `ReflectHelper`, `SimpleRemap`, and more
- **Java NIO Networking** — full NIO server/client stack with packet codec & registry
- **Auto Network Bridge** — auto-connect client via JVM properties

---

## Adding to Your Project

Add the Maven repository and dependency:

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

> Browse packages: [repo.tamkungz.me](https://repo.tamkungz.me)

---

## Quick Start

### Logging

```java
private static final NilLogger LOG = Loggers.sdk();
private static final NilLogger CLASS_LOG = Loggers.forClass(MyClass.class);

LOG.info("SDK initialized");
CLASS_LOG.info("Action executed");
```

### Auto Network Bridge (JVM Properties)

```text
-Dnilloadersdk.network.autoclient.enabled=true
-Dnilloadersdk.network.autoclient.host=127.0.0.1
-Dnilloadersdk.network.autoclient.port=25566
```

Optional tuning:
```text
-Dnilloadersdk.network.autoclient.pollMs=1000
-Dnilloadersdk.network.autoclient.maxFrame=1048576
```

---

## Entrypoint Routing

Entrypoints are resolved in this priority order:

1. JVM property: `-Dnilloadersdk.entrypoint.<phase>=...`
2. Properties file: `nilloadersdk.entrypoints.properties`
3. ServiceLoader modules (`NilLoaderSDKEntrypointModule`)

Never point `premain` or `hijack` to SDK self-entrypoint classes — it will cause infinite recursion.

---

## Build

```bat
gradlew.bat compileJava
```

To publish to local Maven cache:
```bat
gradlew.bat publishToMavenLocal
```

---

## Credits

- [NilLoader (official)](https://git.sleeping.town/Nil/NilLoader)
- [NilLoader (mirror)](https://github.com/exaskye/NilLoader)

---

## License

Licensed under **LGPL-3.0-or-later**.
See the full license text at [gnu.org/licenses/lgpl-3.0.txt](https://www.gnu.org/licenses/lgpl-3.0.txt).