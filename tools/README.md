# External tools

`MinecraftRemapping` is configured as a Git submodule at `tools/MinecraftRemapping`.
The project intentionally does not vendor or relicense that repository's mapping files.

If this source tree came from a ZIP rather than a Git clone, run one of the bootstrap scripts from the repository root:

- Linux/macOS: `./scripts/setup-minecraft-remapping-submodule.sh`
- Windows PowerShell: `./scripts/setup-minecraft-remapping-submodule.ps1`

The scripts pin the submodule to commit `8ca7ba25dfd67eae43b3c73d02603ff6c085a6d7`.
