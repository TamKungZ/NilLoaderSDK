# Mapping policy and tools

NilLoaderSDK 3.x treats Minecraft mapping data as **build input**, not as project-owned source data.

## Why mappings are not bundled

Official Mojang/Microsoft mappings are licensed for development use, but their license says the complete, unmodified mappings may not be redistributed. NilLoaderSDK therefore does not check complete official mapping files into this repository and does not include them in release JARs.

The historical `agaricusb/MinecraftRemapping` repository is attached as an external Git submodule for research and local tooling. Its repository does not currently expose a license file in the GitHub root, so NilLoaderSDK does not copy, vendor, or relicense its mapping collection.

`.remapping/` remains gitignored. A developer can intentionally import a mapping file into that local directory and use it for a local build.

## Submodule

Normal Git clone:

```bash
git clone --recurse-submodules https://github.com/TamKungZ/NilLoaderSDK.git
```

Existing clone:

```bash
git submodule update --init --recursive
```

If the project was obtained as a ZIP, use the setup script once so Git can create the actual gitlink:

```bash
./scripts/setup-minecraft-remapping-submodule.sh
```

PowerShell:

```powershell
./scripts/setup-minecraft-remapping-submodule.ps1
```

## Mapping CLI

The SDK now contains an SRG/CSRG-oriented mapping utility:

```bash
./gradlew mappingTool --args="inspect tools/MinecraftRemapping/1.4.7/mcp2obf.srg"
./gradlew mappingTool --args="reverse input.srg output.srg"
./gradlew mappingTool --args="chain first.srg second.srg output.srg"
./gradlew mappingTool --args="lookup input.srg class net/minecraft/client/Minecraft"
./gradlew mappingTool --args="import-submodule 1.4.7 mcp2obf.srg"
./gradlew mappingTool --args="list-submodule 1.4.7"
./gradlew mappingToolJar
```

`mappingToolJar` builds a standalone Java 8-compatible CLI JAR containing only NilLoaderSDK mapping utility code. No mapping dataset is embedded in that JAR.

`import-submodule` preserves the selected filename under `.remapping/<version>/<mappingFile>`. The build automatically consumes `mcp2obf.srg`; other imported mapping files stay explicit CLI inputs. Everything under `.remapping/` is deliberately ignored by Git.

## Build behavior

A normal CI/release build does **not** import mapping data from the submodule. If `.remapping/<version>/mcp2obf.srg` exists locally, the existing Gradle generation step may extract only names referenced by SDK code into generated Java source for that local build. Do not publish a mapping-derived artifact unless you have verified the license of the mapping source you supplied.
