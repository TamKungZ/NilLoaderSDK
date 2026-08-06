# NilKit API

**Independent community tooling and APIs for NilLoader-based Minecraft development.**

NilKit API provides reusable utilities for mods built around [NilLoader](https://git.sleeping.town/Nil/NilLoader), with a particular focus on legacy Minecraft development.

> [!IMPORTANT]
> **NilKit API is not an official NilLoader project and is not required to use NilLoader.**
>
> NilLoader itself is maintained separately:
> https://git.sleeping.town/Nil/NilLoader

## What does NilKit API provide?

NilKit API collects optional developer-facing utilities that would otherwise need to be reimplemented across multiple nilmods, including:

- Event dispatching and lifecycle helpers
- Reflection and class-patching utilities
- Legacy Minecraft mapping/remapping tooling
- General-purpose KDL utilities
- Java NIO networking helpers
- Standardized logging helpers
- Optional developer-toolbox integrations

The project is intended to **complement NilLoader**, not replace or reimplement it.

## Minecraft compatibility

The core library does not require a hard bytecode link to a specific Minecraft JAR.

Minecraft-facing helpers are resolved lazily and depend on mappings compatible with the target game version. Minecraft `1.4.7` currently has built-in fallback mappings, while additional mapping data can be sourced from the `tools/MinecraftRemapping` submodule during development/build tooling.

See [Minecraft compatibility](docs/compatibility.md).

## Quick build

Gradle itself should run on a supported modern JDK, while the library targets Java 8 bytecode.

Linux/macOS:

```bash
./gradlew clean build
```

Windows:

```bat
gradlew.bat clean build
```

If your system default Java is incompatible with the Gradle version used by the project, see [Building](docs/building.md).

## Documentation

- [Features and API areas](docs/features.md)
- [Minecraft compatibility](docs/compatibility.md)
- [Entrypoints](docs/entrypoints.md)
- [Logging](docs/logging.md)
- [Metadata and KDL](docs/metadata.md)
- [Mappings and remapping](docs/mappings.md)
- [Networking](docs/networking.md)
- [Developer toolbox (`-all.jar`)](docs/toolbox.md)
- [Project architecture](docs/architecture.md)
- [Building and CI](docs/building.md)
- [Project-local Maven publishing](docs/publishing.md)

For release history, see [`CHANGE.md`](CHANGE.md).

## Upstream NilLoader

NilLoader is a separate project and remains under the control of its original maintainers.

- Official repository: https://git.sleeping.town/Nil/NilLoader
- GitHub mirror: https://github.com/exaskye/NilLoader

NilKit API does not claim ownership of NilLoader, its source code, or its branding.

## License

NilKit API is licensed under the **GNU Lesser General Public License v3.0 or later** (`LGPL-3.0-or-later`).

See [`LICENSE`](LICENSE) for the full license text.
