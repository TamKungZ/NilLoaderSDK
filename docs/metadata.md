# Metadata and KDL

NilKit API includes a general-purpose KDL parser/writer and historically also experimented with SDK-specific metadata.

## General-purpose KDL toolkit

The reusable KDL utilities include:

- `KdlParser`
- `KdlWriter`
- `KdlDocument`
- `KdlNode`
- `KdlValue`
- `KdlParseException`

These classes are useful independently of NilLoader metadata.

## Historical SDK-specific metadata

The `3.0.2` codebase contains support for `.nilsdkmod.kdl`, including metadata bridge/bootstrap classes.

This area should be treated as **legacy/experimental during the NilKit migration**.

It should not be presented as a replacement for NilLoader's own metadata/loading machinery, and any implementation that depends on NilLoader internals should be reviewed before being kept in future releases.

Relevant historical classes include:

- `SdkModMetadata`
- `SdkMetadataKdl`
- `SdkMetadataIO`
- `NilMetadataBridge`
- `NilMetadataPatchInstaller`
- `KdlOnlyModBootstrapper`

## Direction going forward

General-purpose KDL parsing can remain a standalone utility.

NilLoader integration should go through supported NilLoader mechanisms rather than sideloading data into internal structures.
