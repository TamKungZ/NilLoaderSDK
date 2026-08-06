# Byte Buddy patches (Mixin-style)

NilKit exposes a composable Byte Buddy patch registry so nilmods can patch game
classes without depending on Sponge Mixin.

```java
import me.tamkungz.nilkit.tooling.ByteBuddyHelper;
import me.tamkungz.nilkit.tooling.bytebuddy.ByteBuddyPatch;
import net.bytebuddy.asm.Advice;

import static me.tamkungz.nilkit.tooling.bytebuddy.MixinMatchers.method;

public final class ExamplePatch {
    public static void register() {
        ByteBuddyHelper.register(ByteBuddyPatch
                .forClass("example.render", "net.minecraft.client.Minecraft")
                .priority(1000)
                .advice(method("runTick"), RunTickAdvice.class)
                .build());
    }

    public static final class RunTickAdvice {
        @Advice.OnMethodEnter
        public static void head() {
            // equivalent to a simple @Inject(at = @At("HEAD"))
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void tail(@Advice.Thrown Throwable thrown) {
            // runs on normal and exceptional exits
        }
    }
}
```

Install the NilLoader bridge during premain/hijack after patches are registered:

```java
ByteBuddyHelper.installNilLoaderBridge();
```

When `Instrumentation` already exists, the same patch definitions can be
installed with retransformation support:

```java
ByteBuddyHelper.installPatches(instrumentation);
```

## Mixin-style capabilities

Byte Buddy `Advice` covers the common HEAD/RETURN/cancellable/modify-argument/
modify-return/exception use cases. `intercept(...)` can replace a method body
using `MethodDelegation`, `MethodCall`, `FixedValue`, etc. For INVOKE/FIELD-like
injection points, use `mutate(...)` with Byte Buddy `MemberSubstitution` or a
custom `AsmVisitorWrapper`.

```java
ByteBuddyPatch.forClass("example.calls", "a.b.Target")
    .mutate(builder -> builder.visit(
        MemberSubstitution.strict()
            .method(named("oldCall"))
            .replaceWith(MyHooks.class.getMethod("replacement", String.class))
            .on(method("tick"))))
    .build();
```

The pre-load NilLoader path can make class-format changes because it rewrites
bytes before definition. Runtime retransformation is intentionally configured
for method-body-safe changes; JVM retransformation cannot reliably add/remove
fields, methods, superclasses, or interfaces on already loaded classes.
