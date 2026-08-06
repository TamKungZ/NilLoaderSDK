package me.tamkungz.nilkit.tooling.bytebuddy;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable, composable Byte Buddy patch definition.
 *
 * <p>This is deliberately closer to a Mixin configuration than to raw
 * Byte Buddy: one target matcher owns an ordered list of mutations. Multiple
 * patches can target the same class and are composed by {@link ByteBuddyPatchRegistry}.</p>
 */
public final class ByteBuddyPatch {

    /** A single ordered mutation of a Byte Buddy type builder. */
    public interface Mutation {
        DynamicType.Builder<?> apply(DynamicType.Builder<?> builder);
    }

    /** Preflight assertion evaluated before bytecode is mutated. */
    public interface Requirement {
        void validate(TypeDescription type);
    }

    private final String id;
    private final int priority;
    private final ElementMatcher<? super TypeDescription> typeMatcher;
    private final List<Mutation> mutations;
    private final List<Requirement> requirements;

    private ByteBuddyPatch(Builder builder) {
        this.id = builder.id;
        this.priority = builder.priority;
        this.typeMatcher = builder.typeMatcher;
        this.mutations = Collections.unmodifiableList(new ArrayList<Mutation>(builder.mutations));
        this.requirements = Collections.unmodifiableList(new ArrayList<Requirement>(builder.requirements));
    }

    public String getId() {
        return id;
    }

    public int getPriority() {
        return priority;
    }

    public boolean matches(TypeDescription type) {
        return typeMatcher.matches(type);
    }

    public DynamicType.Builder<?> apply(TypeDescription type, DynamicType.Builder<?> builder) {
        for (Requirement requirement : requirements) {
            requirement.validate(type);
        }
        DynamicType.Builder<?> current = builder;
        for (Mutation mutation : mutations) {
            current = mutation.apply(current);
            if (current == null) {
                throw new IllegalStateException("Patch mutation returned null: " + id);
            }
        }
        return current;
    }

    public static Builder builder(String id, ElementMatcher<? super TypeDescription> typeMatcher) {
        return new Builder(id, typeMatcher);
    }

    public static Builder forClass(String id, final String binaryClassName) {
        if (binaryClassName == null || binaryClassName.trim().isEmpty()) {
            throw new IllegalArgumentException("binaryClassName cannot be blank");
        }
        final String normalized = binaryClassName.trim().replace('/', '.');
        return builder(id, new ElementMatcher<TypeDescription>() {
            @Override
            public boolean matches(TypeDescription target) {
                return normalized.equals(target.getName());
            }
        });
    }

    public static final class Builder {
        private final String id;
        private final ElementMatcher<? super TypeDescription> typeMatcher;
        private final List<Mutation> mutations = new ArrayList<Mutation>();
        private final List<Requirement> requirements = new ArrayList<Requirement>();
        private int priority = 1000;

        private Builder(String id, ElementMatcher<? super TypeDescription> typeMatcher) {
            if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("id cannot be blank");
            if (typeMatcher == null) throw new IllegalArgumentException("typeMatcher cannot be null");
            this.id = id.trim();
            this.typeMatcher = typeMatcher;
        }

        /**
         * Lower values run first. This mirrors the useful part of Mixin priority
         * while keeping ordering deterministic across different mods/patches.
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /** Requires exactly one declared method/constructor to match. */
        public Builder require(final ElementMatcher<? super MethodDescription> matcher) {
            return require(matcher, 1, 1);
        }

        /**
         * Mixin-like require/expect guard. Transformation fails immediately when
         * the target shape is outside the requested range.
         */
        public Builder require(final ElementMatcher<? super MethodDescription> matcher,
                               final int minimum,
                               final int maximum) {
            if (matcher == null) throw new IllegalArgumentException("matcher cannot be null");
            if (minimum < 0) throw new IllegalArgumentException("minimum cannot be negative");
            if (maximum < minimum) throw new IllegalArgumentException("maximum cannot be smaller than minimum");
            requirements.add(new Requirement() {
                @Override
                public void validate(TypeDescription type) {
                    int count = type.getDeclaredMethods().filter(matcher).size();
                    if (count < minimum || count > maximum) {
                        throw new IllegalStateException("Patch " + id + " expected " + minimum + ".." + maximum
                                + " matching declared methods in " + type.getName() + " but found " + count);
                    }
                }
            });
            return this;
        }

        /**
         * Applies Byte Buddy Advice to matched methods/constructors.
         *
         * <p>Advice is the preferred Mixin-style HEAD/RETURN mechanism. It can
         * modify arguments on enter, skip an original method, alter return values,
         * inspect/replace thrown exceptions, and works for constructors where
         * normal MethodDelegation is not appropriate.</p>
         */
        public Builder advice(final ElementMatcher<? super MethodDescription> methodMatcher,
                              final Class<?> adviceClass) {
            if (methodMatcher == null) throw new IllegalArgumentException("methodMatcher cannot be null");
            if (adviceClass == null) throw new IllegalArgumentException("adviceClass cannot be null");
            return mutate(new Mutation() {
                @Override
                public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder) {
                    return builder.visit(Advice.to(adviceClass).on(methodMatcher));
                }
            });
        }

        /**
         * Replaces method implementations using any Byte Buddy Implementation,
         * e.g. MethodDelegation, FixedValue, SuperMethodCall, or MethodCall.
         */
        public Builder intercept(final ElementMatcher<? super MethodDescription> methodMatcher,
                                 final Implementation implementation) {
            if (methodMatcher == null) throw new IllegalArgumentException("methodMatcher cannot be null");
            if (implementation == null) throw new IllegalArgumentException("implementation cannot be null");
            return mutate(new Mutation() {
                @Override
                public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder) {
                    return builder.method(methodMatcher).intercept(implementation);
                }
            });
        }

        /**
         * Adds a completely custom transformation. This is the escape hatch for
         * MemberSubstitution, custom ASM visitors, field definitions, interfaces,
         * method definitions and any Byte Buddy feature not wrapped here.
         */
        public Builder mutate(Mutation mutation) {
            if (mutation == null) throw new IllegalArgumentException("mutation cannot be null");
            mutations.add(mutation);
            return this;
        }

        public ByteBuddyPatch build() {
            if (mutations.isEmpty()) {
                throw new IllegalStateException("Patch has no mutations: " + id);
            }
            return new ByteBuddyPatch(this);
        }
    }
}
