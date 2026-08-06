# Developer toolbox (`-all.jar`)

The optional `-all.jar` variant bundles additional developer libraries without relocating their public packages.

Current bundled runtime tooling:

- Byte Buddy `1.17.6`
- Byte Buddy Agent `1.17.6`
- GEB core `0.5.4`
- ClassGraph `4.8.184`
- SnakeYAML `2.6`

Convenience facades currently live under the historical package:

```text
me.tamkungz.nilkit.tooling
```

Examples include:

- `DeveloperToolbox`
- `ByteBuddyHelper`
- `ClassGraphHelper`
- `YamlHelper`
- `GebHelper`

## Important behavior

Byte Buddy self-attachment is not performed automatically during SDK startup.

`ByteBuddyHelper.installInstrumentation()` is explicit and may be unavailable on JVMs or platforms that restrict self-attach.

SnakeYAML helpers use a safe constructor and reject duplicate keys.

## GEB annotation processor

GEB's annotation processor is build-time only and is intentionally not bundled into the runtime shadow JAR.

A project authoring GEB `@Listen` handlers should add:

```gradle
annotationProcessor 'foo.zaaarf.geb:processor:0.4.9'
```

Use the normal JAR when these bundled developer libraries are unnecessary.
