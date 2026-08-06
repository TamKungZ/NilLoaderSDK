# Networking

NilKit API contains a small Java NIO networking layer.

Current areas include:

- `Connection`
- `NioServer`
- `NioClient`
- `ServerListener`
- `ClientListener`
- `MinecraftAutoNetworkBridge`
- Packet codec/registry/factory helpers

## Automatic Minecraft bridge

The optional auto-client bridge is disabled by default.

Example configuration:

```text
-Dnilkit.network.autoclient.enabled=true
-Dnilkit.minecraft.version=1.4.7
-Dnilkit.network.autoclient.host=127.0.0.1
-Dnilkit.network.autoclient.port=25566
```

Optional settings:

```text
-Dnilkit.network.autoclient.pollMs=1000
-Dnilkit.network.autoclient.maxFrame=1048576
```

If the requested Minecraft mapping is unavailable, the bridge should fail closed and disable itself rather than stopping game startup.
