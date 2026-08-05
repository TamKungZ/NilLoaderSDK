package me.tamkungz.nilloadersdk.network;

import me.tamkungz.nilloadersdk.network.codec.PacketCodec;
import me.tamkungz.nilloadersdk.network.packet.Packet;
import me.tamkungz.nilloadersdk.network.packet.PacketRegistry;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class NioClient implements Runnable {

    private final String host;
    private final int port;
    private final int maxFrameSize;
    private final int maxReconnectAttempts;
    private final long baseReconnectDelayMillis;
    private final PacketRegistry registry;
    private final ClientListener listener;
    private final Queue<Runnable> pendingTasks = new ConcurrentLinkedQueue<Runnable>();

    private volatile boolean running;
    private Selector selector;
    private SocketChannel channel;
    private volatile Connection connection;
    private int reconnectAttempts;
    private long reconnectAtMillis = -1L;

    public NioClient(String host, int port, int maxFrameSize, int maxReconnectAttempts,
                     long baseReconnectDelayMillis, PacketRegistry registry, ClientListener listener) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (maxFrameSize < PacketCodec.PACKET_ID_SIZE) {
            throw new IllegalArgumentException("maxFrameSize must be >= " + PacketCodec.PACKET_ID_SIZE);
        }
        if (maxReconnectAttempts < 0) {
            throw new IllegalArgumentException("maxReconnectAttempts must be >= 0");
        }
        if (baseReconnectDelayMillis < 0L) {
            throw new IllegalArgumentException("baseReconnectDelayMillis must be >= 0");
        }
        if (registry == null) {
            throw new IllegalArgumentException("registry must not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.host = host.trim();
        this.port = port;
        this.maxFrameSize = maxFrameSize;
        this.maxReconnectAttempts = maxReconnectAttempts;
        this.baseReconnectDelayMillis = baseReconnectDelayMillis;
        this.registry = registry;
        this.listener = listener;
    }

    public NioClient(String host, int port, int maxFrameSize, PacketRegistry registry, ClientListener listener) {
        this(host, port, maxFrameSize, 5, 2000L, registry, listener);
    }

    public NioClient(String host, int port, PacketRegistry registry, ClientListener listener) {
        this(host, port, 1024 * 1024, registry, listener);
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }

        selector = Selector.open();
        reconnectAttempts = 0;
        reconnectAtMillis = -1L;
        running = true;
        try {
            openNewChannelAndConnect();
        } catch (IOException e) {
            running = false;
            shutdownNow();
            throw e;
        }
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        reconnectAtMillis = -1L;
        if (selector != null) {
            selector.wakeup();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isConnected() {
        Connection current = connection;
        return current != null && current.isOpen();
    }

    public Connection getConnection() {
        return connection;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    public void send(final Packet packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }
        enqueueTask(new Runnable() {
            @Override
            public void run() {
                Connection current = connection;
                if (current == null || !current.isOpen()) {
                    return;
                }
                try {
                    current.enqueue(PacketCodec.encode(registry, packet));
                    SelectionKey key = current.getChannel().keyFor(selector);
                    if (key != null && key.isValid()) {
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    }
                } catch (Throwable t) {
                    // Packet serialization/registration errors are application errors and must not
                    // tear down an otherwise healthy network connection.
                    notifyException(t);
                }
            }
        });
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
                tryReconnectIfNeeded();
            }
        } catch (IOException ioException) {
            notifyException(ioException);
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
                if (key.isConnectable()) {
                    finishConnect(key);
                }
                if (key.isValid() && key.isReadable()) {
                    readPackets();
                }
                if (key.isValid() && key.isWritable()) {
                    writePackets(key);
                }
            } catch (Throwable t) {
                notifyException(t);
                handleDisconnect(true);
            }
        }
    }

    private void finishConnect(SelectionKey key) throws IOException {
        if (!channel.finishConnect()) {
            return;
        }

        connection = new Connection(channel);
        reconnectAttempts = 0;
        reconnectAtMillis = -1L;
        key.interestOps(SelectionKey.OP_READ);
        key.attach(connection);
        notifyConnected();
    }

    private void readPackets() throws IOException {
        Connection current = connection;
        if (current == null) {
            return;
        }

        ByteBuffer readBuffer = current.getReadBuffer();
        current.ensureReadBufferWritable(1024);

        int read = current.getChannel().read(readBuffer);
        if (read == -1) {
            handleDisconnect(true);
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
                notifyPacket(packet);
            } else {
                notifyUnknownPacket(decoded.getPacketId());
            }
        }

        readBuffer.compact();
        if (!readBuffer.hasRemaining()) {
            current.ensureReadBufferWritable(1024);
        }
    }

    private void writePackets(SelectionKey key) throws IOException {
        Connection current = connection;
        if (current == null) {
            return;
        }

        current.flushOutbound();
        if (!current.hasOutboundData() && key.isValid()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    private void handleDisconnect(boolean scheduleReconnect) {
        boolean wasConnected = connection != null;
        SocketChannel oldChannel = channel;

        connection = null;
        channel = null;

        try {
            SelectionKey key = oldChannel == null || selector == null ? null : oldChannel.keyFor(selector);
            if (key != null) {
                key.cancel();
            }
            if (oldChannel != null) {
                oldChannel.close();
            }
        } catch (IOException ignored) {
        }

        if (wasConnected) {
            notifyDisconnected();
        }

        if (scheduleReconnect && running) {
            scheduleReconnect();
        } else {
            reconnectAtMillis = -1L;
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            reconnectAtMillis = -1L;
            running = false;
            notifyReconnectExhausted(reconnectAttempts);
            if (selector != null) {
                selector.wakeup();
            }
            return;
        }

        reconnectAttempts++;
        long multiplier = reconnectAttempts;
        long delay;
        if (baseReconnectDelayMillis != 0L && multiplier > Long.MAX_VALUE / baseReconnectDelayMillis) {
            delay = Long.MAX_VALUE;
        } else {
            delay = multiplier * baseReconnectDelayMillis;
        }
        long now = System.currentTimeMillis();
        reconnectAtMillis = delay >= Long.MAX_VALUE - now ? Long.MAX_VALUE : now + delay;
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
                notifyException(t);
            }
        }
    }

    private void tryReconnectIfNeeded() {
        if (!running || connection != null || reconnectAtMillis < 0L) {
            return;
        }

        if (System.currentTimeMillis() < reconnectAtMillis) {
            return;
        }

        reconnectAtMillis = -1L;
        try {
            openNewChannelAndConnect();
        } catch (Throwable t) {
            notifyException(t);
            closeChannelOnly();
            scheduleReconnect();
        }
    }

    private void openNewChannelAndConnect() throws IOException {
        closeChannelOnly();
        SocketChannel newChannel = SocketChannel.open();
        boolean success = false;
        try {
            newChannel.configureBlocking(false);
            boolean connectedImmediately = newChannel.connect(new InetSocketAddress(host, port));
            channel = newChannel;
            if (connectedImmediately) {
                Connection immediate = new Connection(newChannel);
                SelectionKey key = newChannel.register(selector, SelectionKey.OP_READ, immediate);
                connection = immediate;
                reconnectAttempts = 0;
                reconnectAtMillis = -1L;
                // Keep the registration referenced before notifying user code.
                if (!key.isValid()) throw new IOException("Socket registration became invalid during connect");
                notifyConnected();
            } else {
                newChannel.register(selector, SelectionKey.OP_CONNECT);
            }
            success = true;
        } finally {
            if (!success) {
                connection = null;
                channel = null;
                try {
                    newChannel.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void closeChannelOnly() {
        SocketChannel old = channel;
        channel = null;
        if (old == null) {
            return;
        }
        try {
            SelectionKey key = selector == null ? null : old.keyFor(selector);
            if (key != null) {
                key.cancel();
            }
            old.close();
        } catch (IOException ignored) {
        }
    }

    private void shutdownNow() {
        handleDisconnect(false);
        pendingTasks.clear();

        try {
            if (selector != null) {
                selector.close();
            }
        } catch (IOException ignored) {
        } finally {
            selector = null;
        }
    }

    private void notifyConnected() {
        try {
            listener.onConnected();
        } catch (Throwable t) {
            notifyException(t);
        }
    }

    private void notifyDisconnected() {
        try {
            listener.onDisconnected();
        } catch (Throwable t) {
            notifyException(t);
        }
    }

    private void notifyPacket(Packet packet) {
        try {
            listener.onPacket(packet);
        } catch (Throwable t) {
            notifyException(t);
        }
    }

    private void notifyUnknownPacket(int packetId) {
        try {
            listener.onUnknownPacket(packetId);
        } catch (Throwable t) {
            notifyException(t);
        }
    }

    private void notifyReconnectExhausted(int attempts) {
        try {
            listener.onReconnectExhausted(attempts);
        } catch (Throwable t) {
            notifyException(t);
        }
    }

    private void notifyException(Throwable throwable) {
        try {
            listener.onException(throwable);
        } catch (Throwable ignored) {
            // A broken error callback must not terminate the selector loop.
        }
    }
}
