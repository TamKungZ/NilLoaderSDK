package me.tamkungz.nilkit.network;

import me.tamkungz.nilkit.helper.McHelper;
import me.tamkungz.nilkit.helper.ReflectHelper;
import me.tamkungz.nilkit.log.Loggers;
import me.tamkungz.nilkit.network.packet.PacketRegistry;
import me.tamkungz.nilkit.remapping.SimpleRemap;
import nilloader.api.NilLogger;

public final class MinecraftAutoNetworkBridge implements Runnable {

    private static final NilLogger LOG = Loggers.sdk();

    private static volatile boolean started;
    private static volatile boolean running;
    private static volatile Thread worker;

    private static volatile NioClient activeClient;
    private static volatile Thread clientThread;
    private static volatile boolean lastInWorld;

    private MinecraftAutoNetworkBridge() {
    }

    public static synchronized void startFromSystemProperties() {
        boolean enabled = Boolean.parseBoolean(System.getProperty("nilkit.network.autoclient.enabled", "false"));
        if (!enabled || started) {
            return;
        }
        started = true;
        running = true;

        worker = new Thread(new MinecraftAutoNetworkBridge(), "NilKit-AutoNetworkBridge");
        worker.setDaemon(true);
        worker.start();

        LOG.info("Auto network bridge enabled");
    }

    @Override
    public void run() {
        final String host = System.getProperty("nilkit.network.autoclient.host", "127.0.0.1");
        final int port = intProp("nilkit.network.autoclient.port", 25566);
        final int pollMs = positiveIntProp("nilkit.network.autoclient.pollMs", 1000);
        final int maxFrame = positiveIntProp("nilkit.network.autoclient.maxFrame", 1024 * 1024);

        final SimpleRemap remap = resolveRemap();
        if (remap == null) {
            synchronized (MinecraftAutoNetworkBridge.class) {
                worker = null;
                running = false;
                started = false;
                lastInWorld = false;
            }
            return;
        }

        while (running) {
            try {
                Object mc = McHelper.getMinecraftSafe(remap);
                boolean inWorld = false;

                if (mc != null) {
                    Object player = McHelper.getLocalPlayer(mc, remap);
                    Object world = ReflectHelper.getFieldSafe(mc, remap.field("Minecraft", "theWorld"));
                    inWorld = player != null && world != null;
                }

                if (inWorld) {
                    // Restart if the NIO client stopped after exhausting reconnect attempts.
                    NioClient current = activeClient;
                    if (!lastInWorld || current == null || !current.isRunning()) {
                        stopClient();
                        startClient(host, port, maxFrame);
                    }
                } else if (lastInWorld || activeClient != null) {
                    stopClient();
                }

                lastInWorld = inWorld;
                Thread.sleep(pollMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            } catch (Throwable t) {
                LOG.warn("Auto network bridge tick failed", t);
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        }

        stopClient();
        synchronized (MinecraftAutoNetworkBridge.class) {
            worker = null;
            running = false;
            started = false;
            lastInWorld = false;
        }
    }

    private static synchronized void startClient(final String host, final int port, final int maxFrame) {
        if (activeClient != null) {
            return;
        }

        final PacketRegistry registry = new PacketRegistry();
        final NioClient client = new NioClient(host, port, maxFrame, registry, new ClientListener() {
            @Override
            public void onConnected() {
                LOG.info("Auto client connected: " + host + ":" + port);
            }

            @Override
            public void onPacket(me.tamkungz.nilkit.network.packet.Packet packet) {
                // packet handling should be registered by mods using this SDK
            }

            @Override
            public void onDisconnected() {
                LOG.info("Auto client disconnected");
            }

            @Override
            public void onException(Throwable throwable) {
                LOG.warn("Auto client exception", throwable);
            }
        });

        try {
            client.start();
            Thread t = new Thread(client, "NilKit-AutoNioClient");
            t.setDaemon(true);
            t.start();

            activeClient = client;
            clientThread = t;
            LOG.info("Auto client started: " + host + ":" + port);
        } catch (Throwable t) {
            LOG.warn("Failed to start auto client", t);
            try {
                client.stop();
            } catch (Throwable ignored) {
            }
            activeClient = null;
            clientThread = null;
        }
    }

    private static synchronized void stopClient() {
        if (activeClient == null) {
            return;
        }
        try {
            activeClient.stop();
        } catch (Throwable ignored) {
        }
        activeClient = null;
        clientThread = null;
        LOG.info("Auto client stopped");
    }

    public static synchronized void stop() {
        running = false;
        Thread w = worker;
        if (w != null) w.interrupt();
        stopClient();
        // started/worker are cleared by the worker's exit path. Keeping them until then
        // prevents a stop+immediate-start race where the old worker clears the new state.
    }

    public static boolean isRunning() {
        return running;
    }

    private static SimpleRemap resolveRemap() {
        String version = System.getProperty("nilkit.minecraft.version");
        if (version == null || version.trim().isEmpty()) {
            version = System.getProperty("nilkit.network.autoclient.minecraftVersion");
        }
        if (version == null || version.trim().isEmpty()) {
            LOG.warn("Auto network bridge is enabled but no Minecraft version was provided. "
                    + "Set -Dnilkit.minecraft.version=<version>; bridge disabled safely.");
            return null;
        }

        try {
            return SimpleRemap.forVersion(version.trim());
        } catch (Throwable t) {
            LOG.warn("No usable mappings for Minecraft " + version + "; auto network bridge disabled safely", t);
            return null;
        }
    }

    private static int positiveIntProp(String key, int fallback) {
        int value = intProp(key, fallback);
        return value > 0 ? value : fallback;
    }

    private static int intProp(String key, int fallback) {
        String raw = System.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
