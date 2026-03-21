package me.tamkungz.nilloadersdk.network.example;

import me.tamkungz.nilloadersdk.network.ClientListener;
import me.tamkungz.nilloadersdk.network.Connection;
import me.tamkungz.nilloadersdk.network.NioClient;
import me.tamkungz.nilloadersdk.network.NioServer;
import me.tamkungz.nilloadersdk.network.ServerListener;
import me.tamkungz.nilloadersdk.network.packet.Packet;
import me.tamkungz.nilloadersdk.network.packet.PacketFactory;
import me.tamkungz.nilloadersdk.network.packet.PacketRegistry;

/**
 * Standalone demo for the simple NIO network layer.
 */
public final class NetworkDemo {

    private NetworkDemo() {
    }

    public static void main(String[] args) throws Exception {
        final PacketRegistry serverRegistry = createRegistry();
        final PacketRegistry clientRegistry = createRegistry();
        final NioServer[] serverRef = new NioServer[1];
        final NioClient[] clientRef = new NioClient[1];

        final NioServer server = new NioServer(25565, serverRegistry, new ServerListener() {
            @Override
            public void onConnected(Connection connection) {
                System.out.println("[server] client connected: " + connection.getChannel());
            }

            @Override
            public void onDisconnected(Connection connection) {
                System.out.println("[server] client disconnected: " + connection.getChannel());
            }

            @Override
            public void onPacket(Connection connection, Packet packet) {
                System.out.println("[server] recv: " + packet);
                serverRef[0].send(connection, packet); // echo
            }

            @Override
            public void onException(Connection connection, Throwable throwable) {
                System.err.println("[server] exception: " + throwable.getMessage());
            }
        });
        serverRef[0] = server;

        server.start();
        Thread serverThread = new Thread(server, "nio-server-thread");
        serverThread.start();

        final NioClient client = new NioClient("127.0.0.1", 25565, clientRegistry, new ClientListener() {
            @Override
            public void onConnected() {
                System.out.println("[client] connected");
                clientRef[0].send(new PingPacket(System.currentTimeMillis()));
            }

            @Override
            public void onDisconnected() {
                System.out.println("[client] disconnected");
            }

            @Override
            public void onPacket(Packet packet) {
                System.out.println("[client] recv: " + packet);
            }

            @Override
            public void onException(Throwable throwable) {
                System.err.println("[client] exception: " + throwable.getMessage());
            }
        });
        clientRef[0] = client;

        client.start();
        Thread clientThread = new Thread(client, "nio-client-thread");
        clientThread.start();

        Thread.sleep(2000L);
        client.stop();
        server.stop();
        clientThread.join();
        serverThread.join();
    }

    private static PacketRegistry createRegistry() {
        PacketRegistry registry = new PacketRegistry();
        registry.register(1, PingPacket.class, new PacketFactory<PingPacket>() {
            @Override
            public PingPacket create() {
                return new PingPacket();
            }
        });
        return registry;
    }
}

