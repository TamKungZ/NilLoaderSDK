# Features and API areas

This page is a high-level index of the main NilKit API modules.

## Event system

The built-in event bus supports:

- Annotation-based listeners via `@SubscribeEvent`
- Typed callback listeners
- Priority ordering
- Cancellable events
- Lifecycle phase events

Example:

```java
public final class MyModEvents {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPhase(PhaseEvent event) {
        // handle phase event
    }
}

NilKit.registerEvents(new MyModEvents());
```

> During the NilKit API rename, public class/package names may still use the historical `NilKit` identifier until the code migration is complete.

## Helpers

Current helper areas include:

- `ReflectHelper`
- `McHelper`
- `PacketHelper`
- `ProxyHelper`
- `NilLoaderHelper`
- `TransformerHelper`

These cover reflection, Minecraft-specific lookup, packet helpers, NilLoader-facing convenience methods, and transformer registration.

## Class patching

`TransformerHelper` provides convenience wrappers around NilLoader transformer APIs for raw bytecode or ASM `ClassNode` edits.

Transformer patches should be registered during the appropriate NilLoader lifecycle phase before transformer registration is frozen.

## General-purpose utilities

The project also contains utilities such as:

- `TargetFinder`
- `CooldownTracker`
- KDL parser/writer/document model
- Mapping models and command-line tools

See the dedicated documentation pages for details.
