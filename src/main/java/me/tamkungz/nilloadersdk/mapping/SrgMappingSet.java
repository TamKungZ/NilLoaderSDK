package me.tamkungz.nilloadersdk.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory representation of SRG mappings.
 * Names use JVM internal slash form, matching SRG files.
 */
public final class SrgMappingSet {

    private final Map<String, String> packages = new LinkedHashMap<String, String>();
    private final Map<String, String> classes = new LinkedHashMap<String, String>();
    private final Map<String, MemberMapping> fields = new LinkedHashMap<String, MemberMapping>();
    private final Map<String, MethodMapping> methods = new LinkedHashMap<String, MethodMapping>();
    private final List<String> warnings = new ArrayList<String>();

    public Map<String, String> getPackages() {
        return Collections.unmodifiableMap(packages);
    }

    public Map<String, String> getClasses() {
        return Collections.unmodifiableMap(classes);
    }

    public List<MemberMapping> getFields() {
        return Collections.unmodifiableList(new ArrayList<MemberMapping>(fields.values()));
    }

    public List<MethodMapping> getMethods() {
        return Collections.unmodifiableList(new ArrayList<MethodMapping>(methods.values()));
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public int size() {
        return packages.size() + classes.size() + fields.size() + methods.size();
    }

    void putPackage(String source, String target, int line) {
        putUnique(packages, source, target, "package", line);
    }

    void putClass(String source, String target, int line) {
        putUnique(classes, source, target, "class", line);
    }

    void putField(MemberMapping mapping, int line) {
        String key = mapping.sourceKey();
        MemberMapping old = fields.get(key);
        if (old != null && !old.equals(mapping)) {
            warnings.add("line " + line + ": conflicting field mapping for " + key);
            return;
        }
        fields.put(key, mapping);
    }

    void putMethod(MethodMapping mapping, int line) {
        String key = mapping.sourceKey();
        MethodMapping old = methods.get(key);
        if (old != null && !old.equals(mapping)) {
            warnings.add("line " + line + ": conflicting method mapping for " + key);
            return;
        }
        methods.put(key, mapping);
    }

    void warn(String warning) {
        warnings.add(warning);
    }

    private void putUnique(Map<String, String> map, String source, String target, String kind, int line) {
        String old = map.get(source);
        if (old != null && !old.equals(target)) {
            warnings.add("line " + line + ": conflicting " + kind + " mapping for " + source);
            return;
        }
        map.put(source, target);
    }

    public String mapClass(String source) {
        return classes.get(source);
    }

    public MemberMapping mapField(String owner, String name) {
        return fields.get(MemberMapping.key(owner, name));
    }

    public MethodMapping mapMethod(String owner, String name, String descriptor) {
        return methods.get(MethodMapping.key(owner, name, descriptor));
    }

    public static class MemberMapping {
        private final String sourceOwner;
        private final String sourceName;
        private final String targetOwner;
        private final String targetName;

        public MemberMapping(String sourceOwner, String sourceName, String targetOwner, String targetName) {
            this.sourceOwner = sourceOwner;
            this.sourceName = sourceName;
            this.targetOwner = targetOwner;
            this.targetName = targetName;
        }

        public String getSourceOwner() { return sourceOwner; }
        public String getSourceName() { return sourceName; }
        public String getTargetOwner() { return targetOwner; }
        public String getTargetName() { return targetName; }

        String sourceKey() { return key(sourceOwner, sourceName); }
        String targetKey() { return key(targetOwner, targetName); }

        static String key(String owner, String name) {
            return owner + "/" + name;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof MemberMapping)) return false;
            MemberMapping other = (MemberMapping) obj;
            return sourceOwner.equals(other.sourceOwner)
                    && sourceName.equals(other.sourceName)
                    && targetOwner.equals(other.targetOwner)
                    && targetName.equals(other.targetName);
        }

        @Override
        public int hashCode() {
            int result = sourceOwner.hashCode();
            result = 31 * result + sourceName.hashCode();
            result = 31 * result + targetOwner.hashCode();
            result = 31 * result + targetName.hashCode();
            return result;
        }
    }

    public static final class MethodMapping extends MemberMapping {
        private final String sourceDescriptor;
        private final String targetDescriptor;

        public MethodMapping(String sourceOwner, String sourceName, String sourceDescriptor,
                             String targetOwner, String targetName, String targetDescriptor) {
            super(sourceOwner, sourceName, targetOwner, targetName);
            this.sourceDescriptor = sourceDescriptor;
            this.targetDescriptor = targetDescriptor;
        }

        public String getSourceDescriptor() { return sourceDescriptor; }
        public String getTargetDescriptor() { return targetDescriptor; }

        @Override
        String sourceKey() {
            return key(getSourceOwner(), getSourceName(), sourceDescriptor);
        }

        String targetKey() {
            return key(getTargetOwner(), getTargetName(), targetDescriptor);
        }

        static String key(String owner, String name, String descriptor) {
            return owner + "/" + name + " " + descriptor;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof MethodMapping)) return false;
            MethodMapping other = (MethodMapping) obj;
            return super.equals(obj)
                    && sourceDescriptor.equals(other.sourceDescriptor)
                    && targetDescriptor.equals(other.targetDescriptor);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + sourceDescriptor.hashCode();
            result = 31 * result + targetDescriptor.hashCode();
            return result;
        }
    }
}
