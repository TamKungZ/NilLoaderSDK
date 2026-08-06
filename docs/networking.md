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
-Dnilloadersdk.network.autoclient.enabled=true
-Dnilloadersdk.minecraft.version=1.4.7
-Dnilloadersdk.network.autoclient.host=127.0.0.1
-Dnilloadersdk.network.autoclient.port=25566
```

Optional settings:

```text
-Dnilloadersdk.network.autoclient.pollMs=1000
-Dnilloadersdk.network.autoclient.maxFrame=1048576
```

If the requested Minecraft mapping is unavailable, the bridge should fail closed and disable itself rather than stopping game startup.
