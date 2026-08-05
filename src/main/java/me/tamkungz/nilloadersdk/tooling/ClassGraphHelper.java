package me.tamkungz.nilloadersdk.tooling;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small ClassGraph facade for common mod-discovery jobs. The methods return
 * class names instead of ClassGraph-specific model objects so callers can keep
 * their own API surface simple.
 */
public final class ClassGraphHelper {

    private ClassGraphHelper() {
    }

    public static List<String> allClasses(String... acceptedPackages) {
        try (ScanResult scan = graph(acceptedPackages).enableClassInfo().scan()) {
            return immutable(scan.getAllClasses().getNames());
        }
    }

    public static List<String> classesWithAnnotation(String annotationClassName, String... acceptedPackages) {
        requireName(annotationClassName, "annotationClassName");
        try (ScanResult scan = graph(acceptedPackages)
                .enableClassInfo()
                .enableAnnotationInfo()
                .scan()) {
            return immutable(scan.getClassesWithAnnotation(annotationClassName).getNames());
        }
    }

    public static List<String> classesImplementing(String interfaceClassName, String... acceptedPackages) {
        requireName(interfaceClassName, "interfaceClassName");
        try (ScanResult scan = graph(acceptedPackages).enableClassInfo().scan()) {
            return immutable(scan.getClassesImplementing(interfaceClassName).getNames());
        }
    }

    public static List<String> subclassesOf(String superClassName, String... acceptedPackages) {
        requireName(superClassName, "superClassName");
        try (ScanResult scan = graph(acceptedPackages).enableClassInfo().scan()) {
            return immutable(scan.getSubclasses(superClassName).getNames());
        }
    }

    /** Same helpers, but force ClassGraph to inspect through one class loader. */
    public static List<String> allClasses(ClassLoader loader, String... acceptedPackages) {
        ClassGraph graph = graph(acceptedPackages).enableClassInfo();
        if (loader != null) graph = graph.overrideClassLoaders(loader);
        try (ScanResult scan = graph.scan()) {
            return immutable(scan.getAllClasses().getNames());
        }
    }

    private static ClassGraph graph(String... acceptedPackages) {
        ClassGraph graph = new ClassGraph();
        if (acceptedPackages != null && acceptedPackages.length > 0) {
            List<String> cleaned = new ArrayList<String>();
            for (String pkg : acceptedPackages) {
                if (pkg != null && !pkg.trim().isEmpty()) cleaned.add(pkg.trim());
            }
            if (!cleaned.isEmpty()) {
                graph = graph.acceptPackages(cleaned.toArray(new String[cleaned.size()]));
            }
        }
        return graph;
    }

    private static List<String> immutable(List<String> names) {
        return Collections.unmodifiableList(new ArrayList<String>(names));
    }

    private static void requireName(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
