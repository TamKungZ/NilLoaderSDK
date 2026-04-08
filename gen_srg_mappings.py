#!/usr/bin/env python3
"""
gen_srg_mappings.py
-------------------
Reads SRG files from .remapping/<version>/ folders and generates
GeneratedSrgMappings.java into the target source directory.

Priority:
1. mcp2obf.srg
2. obf2mcp.srg (reversed automatically)

Usage:
    python gen_srg_mappings.py

Mapping Credit: https://github.com/agaricusb/MinecraftRemapping
"""

import re
import time
from pathlib import Path

# ── Config ────────────────────────────────────────────────────────────────────
REMAPPING_DIR = Path(r"G:\Projects\Code\Java\Minecraft\NilLoaderSDK\.remapping")
OUTPUT_DIR    = Path(r"G:\Projects\Code\Java\Minecraft\NilLoaderSDK\src\main\java\me\tamkungz\remapping")
PACKAGE       = "me.tamkungz.remapping"
CLASS_NAME    = "GeneratedSrgMappings"

PRIMARY_SRG   = "mcp2obf.srg"
FALLBACK_SRG  = "obf2mcp.srg"
# ─────────────────────────────────────────────────────────────────────────────


def simple_class(internal: str) -> str:
    return internal.rsplit("/", 1)[-1]


def split_owner_name(owner_name: str):
    idx = owner_name.rfind("/")
    if idx <= 0 or idx >= len(owner_name) - 1:
        return None
    owner = owner_name[:idx]
    name = owner_name[idx + 1:]
    return simple_class(owner), name


def parse_srg(srg_path: Path, reverse: bool = False):
    classes = {}
    fields = {}
    methods = {}

    with open(srg_path, encoding="utf-8", errors="replace") as f:
        for raw in f:
            line = raw.strip()
            if not line or line.startswith("#"):
                continue

            if line.startswith("CL: "):
                parts = line[4:].split()
                if len(parts) >= 2:
                    left = parts[0]
                    right = parts[1]

                    if reverse:
                        obf_path = left
                        friendly_path = right
                    else:
                        friendly_path = left
                        obf_path = right

                    friendly = simple_class(friendly_path)
                    obf = obf_path.replace("/", ".")
                    classes[friendly] = obf

            elif line.startswith("FD: "):
                parts = line[4:].split()
                if len(parts) >= 2:
                    left = split_owner_name(parts[0])
                    right = split_owner_name(parts[1])

                    if left and right:
                        if reverse:
                            obf_side = left
                            friendly_side = right
                        else:
                            friendly_side = left
                            obf_side = right

                        key = f"{friendly_side[0]}.{friendly_side[1]}"
                        fields[key] = obf_side[1]

            elif line.startswith("MD: "):
                parts = line[4:].split()
                if len(parts) >= 4:
                    left = split_owner_name(parts[0])
                    right = split_owner_name(parts[2])

                    if left and right:
                        if reverse:
                            obf_side = left
                            friendly_side = right
                        else:
                            friendly_side = left
                            obf_side = right

                        key = f"{friendly_side[0]}.{friendly_side[1]}"
                        methods[key] = obf_side[1]

    return classes, fields, methods


def java_string(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def fmt_num(n: int) -> str:
    return f"{n:,}"


def rule(char="─", width=78):
    return char * width


def collect_versions():
    versions = {}
    if not REMAPPING_DIR.exists():
        raise FileNotFoundError(f"REMAPPING_DIR not found: {REMAPPING_DIR}")

    for entry in sorted(REMAPPING_DIR.iterdir()):
        if not entry.is_dir():
            continue

        primary = entry / PRIMARY_SRG
        fallback = entry / FALLBACK_SRG

        if primary.exists():
            versions[entry.name] = {
                "path": primary,
                "reverse": False,
                "source": PRIMARY_SRG,
            }
        elif fallback.exists():
            versions[entry.name] = {
                "path": fallback,
                "reverse": True,
                "source": FALLBACK_SRG,
            }

    return versions


def generate():
    started = time.perf_counter()
    versions = collect_versions()

    if not versions:
        raise RuntimeError(
            f"No {PRIMARY_SRG} or {FALLBACK_SRG} files found under {REMAPPING_DIR}"
        )

    print()
    print("gen_srg_mappings")
    print(rule())
    print(f"Source directory : {REMAPPING_DIR}")
    print(f"Output directory : {OUTPUT_DIR}")
    print(f"Package          : {PACKAGE}")
    print(f"Class            : {CLASS_NAME}")
    print(f"Versions found   : {', '.join(versions.keys())}")
    print()

    lines = []
    lines.append(f"package {PACKAGE};")
    lines.append("")
    lines.append("// AUTO-GENERATED — do not edit by hand.")
    lines.append("// Run gen_srg_mappings.py to regenerate.")
    lines.append("//")
    lines.append(f"// Source priority: .remapping/<version>/{PRIMARY_SRG}")
    lines.append(f"// Fallback       : .remapping/<version>/{FALLBACK_SRG} (reversed)")
    lines.append("// Mapping Credit: https://github.com/agaricusb/MinecraftRemapping")
    lines.append("")
    lines.append(f"final class {CLASS_NAME} {{")
    lines.append("")
    lines.append(f"    private {CLASS_NAME}() {{}}")
    lines.append("")
    lines.append("    /**")
    lines.append("     * Applies mappings for the given version into {@code remap}.")
    lines.append("     * @return true if the version is known, false otherwise.")
    lines.append("     */")
    lines.append("    static boolean apply(String version, SimpleRemap remap, boolean overwriteExisting) {")
    lines.append("        switch (version) {")

    parsed_versions = {}
    total_cls = 0
    total_fld = 0
    total_mth = 0

    print("Parsing mapping files")
    print(rule())

    for ver, info in versions.items():
        path = info["path"]
        reverse = info["reverse"]
        source = info["source"]

        classes, fields, methods = parse_srg(path, reverse=reverse)
        parsed_versions[ver] = (classes, fields, methods)

        total_cls += len(classes)
        total_fld += len(fields)
        total_mth += len(methods)

        print(f"[{ver}]")
        print(f"  Source   : {source}")
        print(f"  File     : {path}")
        print(f"  Classes  : {fmt_num(len(classes))}")
        print(f"  Fields   : {fmt_num(len(fields))}")
        print(f"  Methods  : {fmt_num(len(methods))}")
        print()

        method_name = "apply_" + re.sub(r"[^a-zA-Z0-9]", "_", ver)
        lines.append(f'            case {java_string(ver)}:')
        lines.append(f"                {method_name}(remap, overwriteExisting);")
        lines.append("                return true;")

    lines.append("            default:")
    lines.append("                return false;")
    lines.append("        }")
    lines.append("    }")
    lines.append("")

    for ver, (classes, fields, methods) in parsed_versions.items():
        method_name = "apply_" + re.sub(r"[^a-zA-Z0-9]", "_", ver)

        lines.append(f"    // ── {ver} ───────────────────────────────────────────────────────────────")
        lines.append(f"    private static void {method_name}(SimpleRemap r, boolean ow) {{")

        if classes:
            lines.append("        // classes")
            for friendly, obf in sorted(classes.items()):
                lines.append(f"        r.addClass({java_string(friendly)}, {java_string(obf)}, ow);")

        if fields:
            lines.append("        // fields")
            for key, obf in sorted(fields.items()):
                cls_name, field_name = key.split(".", 1)
                lines.append(
                    f"        r.addField({java_string(cls_name)}, {java_string(field_name)}, {java_string(obf)}, ow);"
                )

        if methods:
            lines.append("        // methods")
            for key, obf in sorted(methods.items()):
                cls_name, method_name_k = key.split(".", 1)
                lines.append(
                    f"        r.addMethod({java_string(cls_name)}, {java_string(method_name_k)}, {java_string(obf)}, ow);"
                )

        lines.append("    }")
        lines.append("")

    lines.append("}")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    out_file = OUTPUT_DIR / f"{CLASS_NAME}.java"
    with open(out_file, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    elapsed = time.perf_counter() - started

    print("Summary")
    print(rule())
    print("Status           : OK")
    print(f"Output file      : {out_file}")
    print(f"Versions         : {fmt_num(len(parsed_versions))}")
    print(f"Total classes    : {fmt_num(total_cls)}")
    print(f"Total fields     : {fmt_num(total_fld)}")
    print(f"Total methods    : {fmt_num(total_mth)}")
    print(f"Elapsed          : {elapsed:.2f}s")
    print()


if __name__ == "__main__":
    generate()