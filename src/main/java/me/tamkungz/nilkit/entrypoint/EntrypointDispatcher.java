package me.tamkungz.nilkit.entrypoint;

import me.tamkungz.nilkit.NilKit;
import me.tamkungz.nilkit.event.lifecycle.PhaseEvent;
import me.tamkungz.nilkit.event.lifecycle.PostEntrypointDispatchEvent;
import me.tamkungz.nilkit.event.lifecycle.PreEntrypointDispatchEvent;
import me.tamkungz.nilkit.log.Loggers;
import nilloader.api.NilLogger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * EntrypointDispatcher — central dispatcher for NilKit entrypoints.
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
 * 1) ServiceLoader (NilKitEntrypointModule)
 * 2) JVM properties (-Dnilkit.entrypoint.<phase>=...)
 * 3) Resource file: /nilkit.entrypoints.properties
 */
final class EntrypointDispatcher {

    private static final NilLogger LOG = Loggers.sdk();
    private static final String RESOURCE = "/nilkit.entrypoints.properties";

    private static final ThreadLocal<Set<String>> ACTIVE_PHASES = new ThreadLocal<Set<String>>() {
        @Override
        protected Set<String> initialValue() {
            return new HashSet<>();
        }
    };

    private EntrypointDispatcher() {}

    static void dispatch(String phase) {
        if (phase == null || phase.trim().isEmpty()) {
            LOG.warn("Skipping entrypoint dispatch with blank phase");
            return;
        }

        String normalizedPhase = phase.trim().toLowerCase(Locale.ROOT);
        if (!"premain".equals(normalizedPhase) && !"hijack".equals(normalizedPhase)) {
            LOG.warn("Skipping unknown entrypoint phase=" + phase);
            return;
        }

        Set<String> active = ACTIVE_PHASES.get();
        if (!active.add(normalizedPhase)) {
            LOG.warn("Skipping re-entrant dispatch for phase=" + normalizedPhase);
            return;
        }

        try {
            // The phase is marked active before events fire so an event listener cannot recurse
            // back into the same phase before the guard is installed.
            if (!NilKit.post(new PreEntrypointDispatchEvent(normalizedPhase))) {
                LOG.warn("Entrypoint dispatch cancelled by event listener for phase=" + normalizedPhase);
                return;
            }

            NilKit.post(new PhaseEvent(normalizedPhase));

            int executed = dispatchModules(normalizedPhase);
            List<String> targets = collectTargets(normalizedPhase);
            for (String cn : targets) {
                if (runTarget(cn, normalizedPhase)) {
                    executed++;
                }
            }

            if (executed == 0) {
                LOG.warn("No entrypoints executed for phase=" + normalizedPhase);
            }

            NilKit.post(new PostEntrypointDispatchEvent(normalizedPhase, executed));
        } finally {
            active.remove(normalizedPhase);
            if (active.isEmpty()) {
                ACTIVE_PHASES.remove();
            }
        }
    }

    private static int dispatchModules(String phase) {
        int count = 0;

        try {
            ServiceLoader<NilKitEntrypointModule> loader =
                    ServiceLoader.load(NilKitEntrypointModule.class, EntrypointDispatcher.class.getClassLoader());

            for (NilKitEntrypointModule module : loader) {
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
        String prop = System.getProperty("nilkit.entrypoint." + phase);
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
            java.lang.reflect.Constructor<?> ctor = c.getDeclaredConstructor();
            if (!ctor.isAccessible()) {
                ctor.setAccessible(true);
            }
            Object inst = ctor.newInstance();

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
            return NilKitPremain.class.getName().equals(className);
        }
        if ("hijack".equals(phase)) {
            return NilKitHijack.class.getName().equals(className);
        }
        return false;
    }
}
