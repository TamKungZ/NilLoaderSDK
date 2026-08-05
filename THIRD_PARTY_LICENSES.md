# Third-party software

NilLoaderSDK itself is licensed under LGPL-3.0-or-later. The optional developer toolbox in `NilLoaderSDK-*-all.jar` contains the upstream libraries listed below **without package relocation**. Their original licenses remain in force.

| Component | Version | Distribution role | License |
| --- | --- | --- | --- |
| Byte Buddy (`net.bytebuddy:byte-buddy`) | 1.17.6 | Bundled in `-all.jar` | Apache-2.0 |
| Byte Buddy Agent (`net.bytebuddy:byte-buddy-agent`) | 1.17.6 | Bundled in `-all.jar` | Apache-2.0 |
| ASM (repackaged inside Byte Buddy) | upstream Byte Buddy dependency | Bundled indirectly inside Byte Buddy's namespace | BSD-3-Clause |
| GEB Core (`foo.zaaarf.geb:core`) | 0.5.4 | Bundled in `-all.jar` | MIT |
| GEB Processor (`foo.zaaarf.geb:processor`) | 0.4.9 | Build-time annotation processor only; not bundled | MIT |
| ClassGraph (`io.github.classgraph:classgraph`) | 4.8.184 | Bundled in `-all.jar` | MIT |
| SnakeYAML (`org.yaml:snakeyaml`) | 2.6 | Bundled in `-all.jar` | Apache-2.0 |
| NilLoader (`com.unascribed:nilloader`) | 1.3.6 | Compile-only / development input; not bundled | See NilLoader upstream |

Notices known to be required/preserved include:

- ClassGraph: Copyright (c) 2022 Luke Hutchison.
- GEB: Copyright (c) 2023 zaaarf.
- ASM: Copyright (c) 2000-2011 INRIA, France Telecom.
- SnakeYAML source files identify Copyright (c) 2008, SnakeYAML under Apache-2.0.
- Byte Buddy is Apache-2.0 and its main distribution repackages ASM under Byte Buddy's own namespace.

Copies of the applicable permissive license texts and MIT copyright notices are included under `licenses/` and copied into `META-INF/nilloadersdk/licenses/` in the shadow JAR. Dependency JAR license/service resources are also preserved by the Shadow build where possible.

Complete Minecraft mapping files are **not** bundled with NilLoaderSDK. Mapping input is read directly from the pinned `tools/MinecraftRemapping` Git submodule and only the names actually referenced by SDK code may be emitted into generated source. The external mapping repository remains under its own copyright and terms.
