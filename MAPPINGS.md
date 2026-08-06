# Mapping policy and tools

NilLoaderSDK 3.x treats Minecraft mapping data as **build input**, not as project-owned source data.

## Why mappings are not bundled

Official Mojang/Microsoft mappings are licensed for development use, but their license says the complete, unmodified mappings may not be redistributed. NilLoaderSDK therefore does not check complete official mapping files into this repository and does not include them in release JARs.

The historical `agaricusb/MinecraftRemapping` repository is attached as an external Git submodule for research and local tooling. Its repository does not currently expose a license file in the GitHub root, so NilLoaderSDK does not copy, vendor, or relicense its mapping collection.

`tools/MinecraftRemapping` is the single mapping source path. It is a Git submodule and is consumed directly; NilLoaderSDK does not create or require a `.remapping` staging directory. `.remapping/` remains ignored only so stale local folders from 3.0.0 cannot be committed accidentally.

## Submodule

Normal Git clone:

```bash
git clone --recurse-submodules git@github.com:NilLoaderSDK/NilLoaderSDK.git
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
./gradlew mappingTool --args="inspect-submodule 1.4.7 mcp2obf.srg"
./gradlew mappingTool --args="submodule-path 1.4.7 mcp2obf.srg"
./gradlew mappingTool --args="list-submodule 1.4.7"
./gradlew mappingToolJar
```

`mappingToolJar` builds a standalone Java 8-compatible CLI JAR containing only NilLoaderSDK mapping utility code. No mapping dataset is embedded in that JAR.

`inspect-submodule` and `submodule-path` operate directly on `tools/MinecraftRemapping/<version>/<mappingFile>`. No mapping files are copied into the main project tree.

## Build behavior

CI/release builds check out the pinned submodule and the Gradle generation step reads `tools/MinecraftRemapping/<version>/mcp2obf.srg` directly. Only names referenced by SDK code are emitted into generated Java source; no complete mapping file is copied into the release tree.
