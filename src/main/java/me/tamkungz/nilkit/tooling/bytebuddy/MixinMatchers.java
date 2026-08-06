package me.tamkungz.nilkit.tooling.bytebuddy;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Small matcher vocabulary for code that is conceptually ported from Mixin.
 * Raw Byte Buddy matchers can always be used instead.
 */
public final class MixinMatchers {

    private MixinMatchers() {}

    public static ElementMatcher.Junction<TypeDescription> type(String binaryName) {
        return ElementMatchers.named(binaryName.replace('/', '.'));
    }

    public static ElementMatcher.Junction<MethodDescription> method(String name) {
        return ElementMatchers.isMethod().and(ElementMatchers.named(name));
    }

    public static ElementMatcher.Junction<MethodDescription> constructor() {
        return ElementMatchers.isConstructor();
    }

    public static ElementMatcher.Junction<MethodDescription> typeInitializer() {
        return ElementMatchers.isTypeInitializer();
    }

    /** Matches a method/constructor by exact JVM descriptor, e.g. (Ljava/lang/String;)V. */
    public static ElementMatcher.Junction<MethodDescription> descriptor(String descriptor) {
        final String expected = descriptor;
        return new ElementMatcher.Junction.AbstractBase<MethodDescription>() {
            @Override
            public boolean matches(MethodDescription target) {
                return expected.equals(target.getDescriptor());
            }
        };
    }

    public static ElementMatcher.Junction<MethodDescription> method(String name, String descriptor) {
        return method(name).and(descriptor(descriptor));
    }
}
