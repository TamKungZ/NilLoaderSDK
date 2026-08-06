# Logging

NilKit API provides convenience logging helpers around NilLoader logging.

## Recommended convention

For a single mod, a shared project root logger may be sufficient.

For multi-mod or library use, prefer explicit per-mod roots and class-scoped loggers.

Example:

```java
private static final NilLogger MOD_LOG = Loggers.forMod("ExampleMod");
private static final NilLogger CLASS_LOG =
        Loggers.forModClass("ExampleMod", ExampleService.class);
```

Avoid embedding bracket prefixes such as `[ExampleMod]` directly in log message text when the logger already provides a namespace.
