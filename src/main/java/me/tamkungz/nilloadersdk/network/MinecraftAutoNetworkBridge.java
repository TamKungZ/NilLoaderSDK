package me.tamkungz.nilloadersdk.network;

import me.tamkungz.nilloadersdk.helper.McHelper;
import me.tamkungz.nilloadersdk.helper.ReflectHelper;
import me.tamkungz.nilloadersdk.log.Loggers;
import me.tamkungz.nilloadersdk.network.packet.PacketRegistry;
import me.tamkungz.remapping.SimpleRemap;
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
        boolean enabled = Boolean.parseBoolean(System.getProperty("nilloadersdk.network.autoclient.enabled", "false"));
        if (!enabled || started) {
            return;
        }
        started = true;
        running = true;

        worker = new Thread(new MinecraftAutoNetworkBridge(), "NilLoaderSDK-AutoNetworkBridge");
        worker.setDaemon(true);
        worker.start();

        LOG.info("Auto network bridge enabled");
    }

    @Override
    public void run() {
        final String host = System.getProperty("nilloadersdk.network.autoclient.host", "127.0.0.1");
        final int port = intProp("nilloadersdk.network.autoclient.port", 25566);
        final int pollMs = intProp("nilloadersdk.network.autoclient.pollMs", 1000);
        final int maxFrame = intProp("nilloadersdk.network.autoclient.maxFrame", 1024 * 1024);

        final SimpleRemap remap = SimpleRemap.forVersion("1.4.7");

        while (running) {
            try {
                Object mc = McHelper.getMinecraftSafe(remap);
                boolean inWorld = false;

                if (mc != null) {
                    Object player = McHelper.getLocalPlayer(mc, remap);
                    Object world = ReflectHelper.getFieldSafe(mc, remap.field("Minecraft", "theWorld"));
                    inWorld = player != null && world != null;
                }

                if (inWorld && !lastInWorld) {
                    startClient(host, port, maxFrame);
                } else if (!inWorld && lastInWorld) {
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
            public void onPacket(me.tamkungz.nilloadersdk.network.packet.Packet packet) {
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
            Thread t = new Thread(client, "NilLoaderSDK-AutoNioClient");
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
