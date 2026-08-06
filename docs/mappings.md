# Mappings and remapping

NilKit API contains SRG/CSRG-oriented mapping utilities and uses an external mapping repository during development.

## Mapping submodule

```ini
[submodule "tools/MinecraftRemapping"]
    path = tools/MinecraftRemapping
    url = https://github.com/agaricusb/MinecraftRemapping.git
```

NilKit API should not vendor a second copy of the mapping dataset. Mapping input is read from the submodule when needed.

## Main mapping classes

- `SimpleRemap`
- `SrgMappingSet`
- `SrgMappings`
- `MappingToolMain`

## Useful tasks

```bash
./gradlew mappingTool --args="inspect tools/MinecraftRemapping/1.4.7/mcp2obf.srg"
./gradlew mappingTool --args="reverse input.srg output.srg"
./gradlew mappingTool --args="chain first.srg second.srg output.srg"
./gradlew inspectMinecraftRemapping -PmcVersion=1.4.7
./gradlew mappingToolJar
```

The standalone mapping tool contains the mapping utility code, not the mapping dataset itself.

For mapping-source redistribution and licensing rules, keep the project's dedicated `mapping/MAPPINGS.md` as the authoritative policy document.
