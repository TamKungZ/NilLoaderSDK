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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

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
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        running = true;
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

    public void send(final Connection connection, final Packet packet) {
        enqueueTask(new Runnable() {
            @Override
            public void run() {
                try {
                    connection.enqueue(PacketCodec.encode(registry, packet));
                    SelectionKey key = connection.getChannel().keyFor(selector);
                    if (key != null && key.isValid()) {
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    }
                } catch (Throwable t) {
                    listener.onException(connection, t);
                    closeConnection(connection);
                }
            }
        });
    }

    public void broadcast(Packet packet) {
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
            listener.onException(null, ioException);
        } finally {
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
                if (key.isReadable()) {
                    Connection connection = (Connection) key.attachment();
                    if (connection != null) {
                        readPackets(connection);
                    }
                }
                if (key.isWritable()) {
                    Connection connection = (Connection) key.attachment();
                    if (connection != null) {
                        writePackets(connection, key);
                    }
                }
            } catch (Throwable t) {
                Connection connection = (Connection) key.attachment();
                listener.onException(connection, t);
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
        client.configureBlocking(false);

        Connection connection = new Connection(client);
        connections.put(client, connection);
        client.register(selector, SelectionKey.OP_READ, connection);
        listener.onConnected(connection);
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
                listener.onPacket(connection, packet);
            }
        }

        readBuffer.compact();
        if (!readBuffer.hasRemaining()) {
            connection.ensureReadBufferWritable(1024);
        }
    }

    private void writePackets(Connection connection, SelectionKey key) throws IOException {
        connection.flushOutbound();
        if (!connection.hasOutboundData()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    private void closeConnection(Connection connection) {
        SocketChannel channel = connection.getChannel();
        connections.remove(channel);

        try {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                key.cancel();
            }
            channel.close();
        } catch (IOException ignored) {
        }

        listener.onDisconnected(connection);
    }

    private void enqueueTask(Runnable task) {
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
            task.run();
        }
    }

    private void shutdownNow() {
        for (Connection connection : new ArrayList<Connection>(connections.values())) {
            closeConnection(connection);
        }

        try {
            if (serverChannel != null) {
                serverChannel.close();
            }
        } catch (IOException ignored) {
        }

        try {
            if (selector != null) {
                selector.close();
            }
        } catch (IOException ignored) {
        }
    }
}

