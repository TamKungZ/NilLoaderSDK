# Minecraft compatibility

NilKit API `3.0.2` no longer has a hard bytecode dependency on one specific Minecraft JAR.

Core systems such as events, KDL utilities, metadata helpers, networking, logging, reflection, and mapping tooling can load independently of Minecraft `1.4.7`.

Minecraft-facing helpers are lazy and reflection-based. They only attempt to resolve Minecraft classes when explicitly called and therefore require mappings and runtime structures compatible with the selected game version.

Minecraft `1.4.7` currently has built-in fallback mappings. Additional mapping subsets can be generated from the pinned `tools/MinecraftRemapping` submodule.

## Auto-network bridge

The optional automatic Minecraft network bridge is disabled by default.

Example:

```text
-Dnilloadersdk.network.autoclient.enabled=true
-Dnilloadersdk.minecraft.version=1.4.7
```

If the requested version or mapping is unavailable, the bridge should disable itself rather than aborting game startup.

## Compatibility principle

NilKit API should avoid treating NilLoader or Minecraft implementation details as stable public APIs.

Code that touches version-specific Minecraft structures should remain isolated behind explicitly version-aware helpers.
