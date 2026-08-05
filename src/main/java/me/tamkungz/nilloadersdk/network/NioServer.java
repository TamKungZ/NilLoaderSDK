package me.tamkungz.nilloadersdk.network;

import me.tamkungz.nilloadersdk.network.codec.PacketCodec;
import me.tamkungz.nilloadersdk.network.packet.Packet;
import me.tamkungz.nilloadersdk.network.packet.PacketRegistry;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class NioServer implements Runnable {

    private final int port;
    private final int maxFrameSize;
    private final PacketRegistry registry;
    private final ServerListener listener;

    private final Map<SocketChannel, Connection> connections = new ConcurrentHashMap<SocketChannel, Connection>();
    private final Queue<Runnable> pendingTasks = new ConcurrentLinkedQueue<Runnable>();

    private volatile boolean running;
    private Selector selector;
    private ServerSocketChannel serverChannel;

    public NioServer(int port, int maxFrameSize, PacketRegistry registry, ServerListener listener) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (maxFrameSize < PacketCodec.PACKET_ID_SIZE) {
            throw new IllegalArgumentException("maxFrameSize must be >= " + PacketCodec.PACKET_ID_SIZE);
        }
        if (registry == null) {
            throw new IllegalArgumentException("registry must not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.port = port;
        this.maxFrameSize = maxFrameSize;
        this.registry = registry;
        this.listener = listener;
    }

    public NioServer(int port, PacketRegistry registry, ServerListener listener) {
        this(port, 1024 * 1024, registry, listener);
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }

        selector = Selector.open();
        boolean success = false;
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            running = true;
            success = true;
        } finally {
            if (!success) {
                running = false;
                shutdownNow();
            }
        }
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (selector != null) {
            selector.wakeup();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getConnectionCount() {
        return connections.size();
    }

    public List<Connection> getConnections() {
        return Collections.unmodifiableList(new ArrayList<Connection>(connections.values()));
    }

    /** Returns the actual bound port. Useful when the server was created with port 0. */
    public int getBoundPort() {
        ServerSocketChannel current = serverChannel;
        if (current == null || !current.isOpen()) {
            return -1;
        }
        try {
            return ((InetSocketAddress) current.getLocalAddress()).getPort();
        } catch (IOException e) {
            return -1;
        }
    }

    public void send(final Connection connection, final Packet packet) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }
        enqueueTask(new Runnable() {
            @Override
            public void run() {
                if (!connections.containsKey(connection.getChannel()) || !connection.isOpen()) {
                    return;
                }
                try {
                    connection.enqueue(PacketCodec.encode(registry, packet));
                    SelectionKey key = connection.getChannel().keyFor(selector);
                    if (key != null && key.isValid()) {
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    }
                } catch (Throwable t) {
                    // Encoding errors are packet/application errors; don't disconnect the client.
                    notifyException(connection, t);
                }
            }
        });
    }

    public void broadcast(Packet packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }
        List<Connection> snapshot = new ArrayList<Connection>(connections.values());
        for (Connection connection : snapshot) {
            send(connection, packet);
        }
    }

    @Override
    public void run() {
        if (!running) {
            throw new IllegalStateException("Call start() before run().");
        }

        try {
            while (running) {
                selector.select(250L);
                runPendingTasks();
                processSelectedKeys();
            }
        } catch (IOException ioException) {
            notifyException(null, ioException);
        } finally {
            running = false;
            shutdownNow();
        }
    }

    private void processSelectedKeys() {
        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();

            if (!key.isValid()) {
                continue;
            }

            try {
                if (key.isAcceptable()) {
                    acceptClient();
                }
                if (key.isValid() && key.isReadable()) {
                    Connection connection = (Connection) key.attachment();
                    if (connection != null) {
                        readPackets(connection);
                    }
                }
                if (key.isValid() && key.isWritable()) {
                    Connection connection = (Connection) key.attachment();
                    if (connection != null) {
                        writePackets(connection, key);
                    }
                }
            } catch (Throwable t) {
                Connection connection = key.attachment() instanceof Connection ? (Connection) key.attachment() : null;
                notifyException(connection, t);
                if (connection != null) {
                    closeConnection(connection);
                }
            }
        }
    }

    private void acceptClient() throws IOException {
        SocketChannel client = serverChannel.accept();
        if (client == null) {
            return;
        }

        Connection connection = null;
        boolean registered = false;
        try {
            client.configureBlocking(false);
            connection = new Connection(client);
            connections.put(client, connection);
            client.register(selector, SelectionKey.OP_READ, connection);
            registered = true;
            notifyConnected(connection);
        } finally {
            if (!registered) {
                if (connection != null) {
                    connections.remove(client, connection);
                }
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void readPackets(Connection connection) throws IOException {
        ByteBuffer readBuffer = connection.getReadBuffer();
        connection.ensureReadBufferWritable(1024);

        int read = connection.getChannel().read(readBuffer);
        if (read == -1) {
            closeConnection(connection);
            return;
        }
        if (read == 0) {
            return;
        }

        readBuffer.flip();
        while (true) {
            PacketCodec.DecodedPacket decoded = PacketCodec.tryDecode(registry, readBuffer, maxFrameSize);
            if (decoded == null) {
                break;
            }

            Packet packet = decoded.getPacket();
            if (packet != null) {
                notifyPacket(connection, packet);
            } else {
                notifyUnknownPacket(connection, decoded.getPacketId());
            }
        }

        readBuffer.compact();
        if (!readBuffer.hasRemaining()) {
            connection.ensureReadBufferWritable(1024);
        }
    }

    private void writePackets(Connection connection, SelectionKey key) throws IOException {
        connection.flushOutbound();
        if (!connection.hasOutboundData() && key.isValid()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    private void closeConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        SocketChannel channel = connection.getChannel();
        if (!connections.remove(channel, connection)) {
            return; // Already closed/not owned by this server: don't fire disconnect twice.
        }

        try {
            SelectionKey key = selector == null ? null : channel.keyFor(selector);
            if (key != null) {
                key.cancel();
            }
            channel.close();
        } catch (IOException ignored) {
        }

        notifyDisconnected(connection);
    }

    private void enqueueTask(Runnable task) {
        if (task == null) {
            return;
        }
        pendingTasks.add(task);
        if (selector != null) {
            selector.wakeup();
        }
    }

    private void runPendingTasks() {
        while (true) {
            Runnable task = pendingTasks.poll();
            if (task == null) {
                return;
            }
            try {
                task.run();
            } catch (Throwable t) {
                notifyException(null, t);
            }
        }
    }

    private void shutdownNow() {
        for (Connection connection : new ArrayList<Connection>(connections.values())) {
            closeConnection(connection);
        }
        pendingTasks.clear();

        try {
            if (serverChannel != null) {
                serverChannel.close();
            }
        } catch (IOException ignored) {
        } finally {
            serverChannel = null;
        }

        try {
            if (selector != null) {
                selector.close();
            }
        } catch (IOException ignored) {
        } finally {
            selector = null;
        }
    }

    private void notifyConnected(Connection connection) {
        try {
            listener.onConnected(connection);
        } catch (Throwable t) {
            notifyException(connection, t);
        }
    }

    private void notifyDisconnected(Connection connection) {
        try {
            listener.onDisconnected(connection);
        } catch (Throwable t) {
            notifyException(connection, t);
        }
    }

    private void notifyPacket(Connection connection, Packet packet) {
        try {
            listener.onPacket(connection, packet);
        } catch (Throwable t) {
            notifyException(connection, t);
        }
    }

    private void notifyUnknownPacket(Connection connection, int packetId) {
        try {
            listener.onUnknownPacket(connection, packetId);
        } catch (Throwable t) {
            notifyException(connection, t);
        }
    }

    private void notifyException(Connection connection, Throwable throwable) {
        try {
            listener.onException(connection, throwable);
        } catch (Throwable ignored) {
            // A broken error callback must not terminate the selector loop.
        }
    }
}
