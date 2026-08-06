<h1>
  <img src="https://raw.githubusercontent.com/NilKit/NilKit/refs/heads/main/src/main/resources/assets/nilkit/icon.svg"
       width="30"
       alt=""
       aria-hidden="true">
  NilKit API
</h1>

**An independent utility API and developer toolkit for mods built with NilLoader.**

<blockquote>
  <p><strong>Important</strong></p>
  <p><strong>NilKit is not part of the official NilLoader project and is not required to use NilLoader.</strong></p>
  <p>NilLoader is maintained separately at:<br>
  <a href="https://git.sleeping.town/Nil/NilLoader">https://git.sleeping.town/Nil/NilLoader</a></p>
</blockquote>

NilKit provides reusable APIs and tooling for legacy Minecraft mod development, with a focus on reducing boilerplate around events, reflection, mappings, metadata, networking, and development utilities.

NilKit `4.1.0` targets Java 8 bytecode and does not require a hard runtime link to a specific Minecraft JAR for its core APIs. Minecraft-specific functionality remains dependent on the mappings and structure of the target game version.

## Features

### Events and lifecycle

- Lightweight event bus
- Cancellable events
- Listener priorities
- `premain` / `hijack` lifecycle helpers
- Typed and annotation-based listeners

### Reflection and legacy Minecraft tooling

- Reflection helpers for version-dependent Minecraft code
- SRG/CSRG mapping utilities
- Mapping inspection, reversal, chaining, and lookup
- Integration with the external `MinecraftRemapping` mapping repository during development

### KDL

NilKit includes a general-purpose KDL parser and writer and can use KDL for richer project/mod metadata.

> *KDL support is optional. NilKit does not require NilLoader mods to use KDL, and standard NilLoader metadata remains fully supported.*

Example:

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

nilkit {
  requires "nilloader" "nilkit"
  load_after "nilkit"

  icon "assets/mymod/icon.png"
  modurl "https://modrinth.com/mod/my-mod"
  sourceurl "https://github.com/example/my-mod"

  license "MIT"
  credits "Author"
}
```

### Networking

* Java NIO client/server utilities
* Packet registry and codecs
* Optional Minecraft-facing network helpers

### Developer toolbox

The optional `-all.jar` also bundles several developer libraries without relocating their public packages:

* Byte Buddy
* GEB
* ClassGraph
* SnakeYAML

The normal NilKit artifact does not require these libraries at runtime.

## Gradle

```gradle
repositories {
    maven {
        url = uri("https://repo.tamkungz.me")
    }
}

dependencies {
    implementation "me.tamkungz.nilkit:nilkit:4.1.0"
}
```

## Documentation

Full documentation and development information are available in the GitHub repository:

[https://github.com/NilKit/NilKit](https://github.com/NilKit/NilKit)

## NilLoader

NilKit is designed to complement NilLoader, not replace it.

NilLoader itself, its loading lifecycle, and its upstream development remain separate from NilKit.

* [NilLoader — official repository](https://git.sleeping.town/Nil/NilLoader)
* [NilLoader — GitHub mirror](https://github.com/exaskye/NilLoader)

## License

NilKit is licensed under **LGPL-3.0-or-later**.
