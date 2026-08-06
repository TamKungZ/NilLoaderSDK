# Project architecture

This page keeps file/module structure out of the main README.

## Core

- `NilModBase`
- build metadata helpers

## Entrypoint system

- `NilKitPremain`
- `NilKitHijack`
- `EntrypointDispatcher`
- `NilKitEntrypointModule`
- `DefaultSdkEntrypointModule`

## Event system

- `NilKit`
- `EventBus`
- `Event`
- `CancellableEvent`
- `SubscribeEvent`
- `EventPriority`

## Helpers

- `ReflectHelper`
- `McHelper`
- `PacketHelper`
- `ProxyHelper`
- `NilLoaderHelper`
- `TransformerHelper`

## KDL / metadata

General KDL toolkit:

- `KdlParser`
- `KdlWriter`
- `KdlDocument`
- `KdlNode`
- `KdlValue`
- `KdlParseException`

Historical SDK-specific metadata integration:

- `SdkModMetadata`
- `SdkMetadataKdl`
- `SdkMetadataIO`
- `NilMetadataBridge`
- `NilMetadataPatchInstaller`
- `KdlOnlyModBootstrapper`

## Mapping

- `SimpleRemap`
- `SrgMappingSet`
- `SrgMappings`
- `MappingToolMain`
- `tools/MinecraftRemapping`

## Networking

- `Connection`
- `NioServer`
- `NioClient`
- listener interfaces
- packet codec/registry/factory
- `MinecraftAutoNetworkBridge`

## Rename note

The project is transitioning from **NilKit** to **NilKit API**.

The documentation can use the new public project name immediately, while code/package/resource identifiers should be migrated in one controlled change so the repository does not end up with a half-renamed API.
