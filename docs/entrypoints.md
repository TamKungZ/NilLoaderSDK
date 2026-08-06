# Entrypoints

NilKit API currently contains an entrypoint routing layer for `premain` and `hijack` phases.

Entrypoints are resolved in this order:

1. JVM property
2. Properties file
3. `ServiceLoader` modules

Historical JVM property format:

```text
-Dnilkit.entrypoint.<phase>=...
```

The default properties resource is:

```text
src/main/resources/nilkit.entrypoints.properties
```

## Important

Do not point a configured phase back at the SDK's own dispatcher entrypoint classes or you may create recursive dispatch.

## Migration note

The project is being renamed from NilKit to NilKit API. Entrypoint class names, package names, resource names, and JVM property prefixes should be migrated deliberately rather than renamed partially.
