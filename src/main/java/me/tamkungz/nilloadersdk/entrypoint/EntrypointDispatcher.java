package me.tamkungz.nilloadersdk.entrypoint;

import me.tamkungz.nilloadersdk.log.Loggers;
import nilloader.api.NilLogger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * EntrypointDispatcher — central dispatcher for NilLoaderSDK entrypoints.
 *
 * Responsibilities:
 * - Prevent re-entrant execution per phase
 * - Execute ServiceLoader modules
 * - Load and execute configured entrypoint classes
 *
 * Supported phases:
 * - "premain"
 * - "hijack"
 *
 * Entrypoints can be registered via:
 * 1) ServiceLoader (NilLoaderSDKEntrypointModule)
 * 2) JVM properties (-Dnilloadersdk.entrypoint.<phase>=...)
 * 3) Resource file: /nilloadersdk.entrypoints.properties
 */
final class EntrypointDispatcher {

    private static final NilLogger LOG = Loggers.sdk();
    private static final String RESOURCE = "/nilloadersdk.entrypoints.properties";

    private static final ThreadLocal<Set<String>> ACTIVE_PHASES = new ThreadLocal<Set<String>>() {
        @Override
        protected Set<String> initialValue() {
            return new HashSet<>();
        }
    };

    private EntrypointDispatcher() {}

    static void dispatch(String phase) {
        Set<String> active = ACTIVE_PHASES.get();
        if (active.contains(phase)) {
            LOG.warn("Skipping re-entrant dispatch for phase=" + phase);
            return;
        }

        active.add(phase);
        int executed = 0;

        try {
            executed += dispatchModules(phase);

            List<String> targets = collectTargets(phase);
            for (String cn : targets) {
                if (runTarget(cn, phase)) {
                    executed++;
                }
            }

            if (executed == 0) {
                LOG.warn("No entrypoints executed for phase=" + phase);
            }
        } finally {
            active.remove(phase);
            if (active.isEmpty()) {
                ACTIVE_PHASES.remove();
            }
        }
    }

    private static int dispatchModules(String phase) {
        int count = 0;

        try {
            ServiceLoader<NilLoaderSDKEntrypointModule> loader =
                    ServiceLoader.load(NilLoaderSDKEntrypointModule.class, EntrypointDispatcher.class.getClassLoader());

            for (NilLoaderSDKEntrypointModule module : loader) {
                try {
                    if ("premain".equals(phase)) {
                        module.onPremain();
                    } else if ("hijack".equals(phase)) {
                        module.onHijack();
                    } else {
                        LOG.warn("Unknown phase=" + phase);
                        return count;
                    }

                    count++;
                    LOG.info("Module executed: " + module.getId() + " (phase=" + phase + ")");

                } catch (Throwable t) {
                    LOG.error("Module failed: " + module.getId() + " (phase=" + phase + ")", t);
                }
            }

        } catch (Throwable t) {
            LOG.warn("Failed to load ServiceLoader modules", t);
        }

        return count;
    }

    private static List<String> collectTargets(String phase) {
        List<String> out = new ArrayList<>();

        // 1) JVM property override
        String prop = System.getProperty("nilloadersdk.entrypoint." + phase);
        addCsv(out, prop);

        // 2) resource file fallback
        Properties p = new Properties();
        try (InputStream in = EntrypointDispatcher.class.getResourceAsStream(RESOURCE)) {
            if (in != null) {
                p.load(in);
                addCsv(out, p.getProperty(phase));
            }
        } catch (Throwable t) {
            LOG.warn("Failed to read " + RESOURCE, t);
        }

        return out;
    }

    private static void addCsv(List<String> out, String csv) {
        if (csv == null || csv.trim().isEmpty()) return;

        for (String raw : csv.split(",")) {
            String s = raw.trim();
            if (!s.isEmpty() && !out.contains(s)) {
                out.add(s);
            }
        }
    }

    private static boolean runTarget(String className, String phase) {
        if (isSelfEntrypoint(className, phase)) {
            LOG.warn("Skipping self entrypoint target " + className + " (phase=" + phase + ")");
            return false;
        }

        try {
            Class<?> c = Class.forName(className);
            Object inst = c.newInstance();

            if (!(inst instanceof Runnable)) {
                LOG.error("Target is not Runnable: " + className + " (phase=" + phase + ")");
                return false;
            }

            ((Runnable) inst).run();
            return true;

        } catch (Throwable t) {
            LOG.error("Failed to run target " + className + " (phase=" + phase + ")", t);
            return false;
        }
    }

    private static boolean isSelfEntrypoint(String className, String phase) {
        if ("premain".equals(phase)) {
            return NilLoaderSDKPremain.class.getName().equals(className);
        }
        if ("hijack".equals(phase)) {
            return NilLoaderSDKHijack.class.getName().equals(className);
        }
        return false;
    }
}