package me.tamkungz.nilloadersdk.metadata;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads/writes SDK-only metadata from .nilsdkmod.kdl files.
 */
public final class SdkMetadataIO {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public static final String DEFAULT_METADATA_FILE = "nilloadersdk.nilsdkmod.kdl";
    public static final String LEGACY_METADATA_FILE = "nilloadersdk.metadata.kdl";
    public static final String LEGACY_METADATA_FILE_2 = "nilloadersdk.kdl";

    private SdkMetadataIO() {}

    public static Optional<SdkModMetadata> readFromSource(File source) {
        return readFromSource(source, null);
    }

    public static Optional<SdkModMetadata> readFromSource(File source, String modId) {
        if (source == null || !source.exists()) return Optional.empty();
        try {
            if (source.isDirectory()) {
                return readFromDirectory(source, modId);
            }
            return readFromJar(source, modId);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    public static Optional<SdkModMetadata> readFromFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) return Optional.empty();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            String text = slurpUtf8(fis);
            SdkModMetadata metadata = SdkMetadataKdl.parse(text);
            return metadata.isEmpty() ? Optional.<SdkModMetadata>empty() : Optional.of(metadata);
        } catch (Throwable t) {
            return Optional.empty();
        } finally {
            closeQuietly(fis);
        }
    }

    public static boolean writeToFile(File file, SdkModMetadata metadata) {
        if (file == null) return false;
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) return false;

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(SdkMetadataKdl.write(metadata).getBytes(UTF_8));
            fos.flush();
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            closeQuietly(fos);
        }
    }

    private static Optional<SdkModMetadata> readFromDirectory(File dir, String modId) {
        File main = new File(dir, chooseFileName(modId));
        Optional<SdkModMetadata> metadata = readFromFile(main);
        if (metadata.isPresent()) return metadata;

        File legacy = new File(dir, LEGACY_METADATA_FILE);
        metadata = readFromFile(legacy);
        if (metadata.isPresent()) return metadata;

        File legacy2 = new File(dir, LEGACY_METADATA_FILE_2);
        metadata = readFromFile(legacy2);
        if (metadata.isPresent()) return metadata;

        File[] all = dir.listFiles();
        if (all != null) {
            for (File f : all) {
                if (f == null || !f.isFile()) continue;
                String n = f.getName().toLowerCase();
                if (!n.endsWith(".nilsdkmod.kdl")) continue;
                metadata = readFromFile(f);
                if (metadata.isPresent()) return metadata;
            }
        }
        return Optional.empty();
    }

    private static Optional<SdkModMetadata> readFromJar(File jar, String modId) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(jar);
            String text = readEntryUtf8(zip, chooseFileName(modId));
            if (text == null) text = readEntryUtf8(zip, LEGACY_METADATA_FILE);
            if (text == null) text = readEntryUtf8(zip, LEGACY_METADATA_FILE_2);
            if (text == null) {
                // fallback: case-insensitive suffix scan for compatibility
                Enumeration<? extends ZipEntry> en = zip.entries();
                while (en.hasMoreElements()) {
                    ZipEntry ze = en.nextElement();
                    if (ze.isDirectory()) continue;
                    String name = ze.getName();
                    String lower = name.toLowerCase();
                    if (lower.endsWith("/" + DEFAULT_METADATA_FILE)
                            || lower.endsWith("/" + LEGACY_METADATA_FILE)
                            || lower.endsWith("/" + LEGACY_METADATA_FILE_2)
                            || lower.endsWith(".nilsdkmod.kdl")) {
                        text = readEntryUtf8(zip, name);
                        if (text != null) break;
                    }
                }
            }

            if (text == null) return Optional.empty();
            SdkModMetadata metadata = SdkMetadataKdl.parse(text);
            return metadata.isEmpty() ? Optional.<SdkModMetadata>empty() : Optional.of(metadata);
        } catch (Throwable t) {
            return Optional.empty();
        } finally {
            closeQuietly(zip);
        }
    }

    private static String readEntryUtf8(ZipFile zip, String entryName) throws IOException {
        ZipEntry ze = zip.getEntry(entryName);
        if (ze == null || ze.isDirectory()) return null;

        InputStream in = null;
        try {
            in = zip.getInputStream(ze);
            return slurpUtf8(in);
        } finally {
            closeQuietly(in);
        }
    }

    private static String slurpUtf8(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) >= 0) {
            baos.write(buf, 0, r);
        }
        return decodeText(baos.toByteArray());
    }

    private static String decodeText(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";

        // UTF-8 BOM
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }

        // UTF-16 LE BOM
        if (bytes.length >= 2
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xFE) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }

        // UTF-16 BE BOM
        if (bytes.length >= 2
                && (bytes[0] & 0xFF) == 0xFE
                && (bytes[1] & 0xFF) == 0xFF) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        }

        // UTF-16 LE (heuristic, no BOM)
        if (looksLikeUtf16Le(bytes)) {
            return new String(bytes, StandardCharsets.UTF_16LE);
        }

        // UTF-16 BE (heuristic, no BOM)
        if (looksLikeUtf16Be(bytes)) {
            return new String(bytes, StandardCharsets.UTF_16BE);
        }

        // Fallback: UTF-8
        return new String(bytes, UTF_8);
    }

    private static boolean looksLikeUtf16Le(byte[] bytes) {
        int sample = Math.min(bytes.length, 64);
        int zerosOnOdd = 0;
        int checked = 0;
        for (int i = 1; i < sample; i += 2) {
            checked++;
            if (bytes[i] == 0) zerosOnOdd++;
        }
        return checked >= 4 && zerosOnOdd >= checked - 1;
    }

    private static boolean looksLikeUtf16Be(byte[] bytes) {
        int sample = Math.min(bytes.length, 64);
        int zerosOnEven = 0;
        int checked = 0;
        for (int i = 0; i < sample; i += 2) {
            checked++;
            if (bytes[i] == 0) zerosOnEven++;
        }
        return checked >= 4 && zerosOnEven >= checked - 1;
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable ignored) {
        }
    }

    private static String chooseFileName(String modId) {
        if (modId == null || modId.trim().isEmpty()) return DEFAULT_METADATA_FILE;
        return modId.trim() + ".nilsdkmod.kdl";
    }
}

