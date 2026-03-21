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
    private Connection connection;
    private int reconnectAttempts;
    private long reconnectAtMillis = -1L;

    public NioClient(String host, int port, int maxFrameSize, int maxReconnectAttempts,
                     long baseReconnectDelayMillis, PacketRegistry registry, ClientListener listener) {
        this.host = host;
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
        openNewChannelAndConnect();
        running = true;
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

    public void send(final Packet packet) {
        enqueueTask(new Runnable() {
            @Override
            public void run() {
                if (connection == null) {
                    return;
                }
                try {
                    connection.enqueue(PacketCodec.encode(registry, packet));
                    SelectionKey key = connection.getChannel().keyFor(selector);
                    if (key != null && key.isValid()) {
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    }
                } catch (Throwable t) {
                    listener.onException(t);
                    handleDisconnect(true);
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
            listener.onException(ioException);
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
                if (key.isConnectable()) {
                    finishConnect(key);
                }
                if (key.isReadable()) {
                    readPackets();
                }
                if (key.isWritable()) {
                    writePackets(key);
                }
            } catch (Throwable t) {
                listener.onException(t);
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
        listener.onConnected();
    }

    private void readPackets() throws IOException {
        if (connection == null) {
            return;
        }

        ByteBuffer readBuffer = connection.getReadBuffer();
        connection.ensureReadBufferWritable(1024);

        int read = connection.getChannel().read(readBuffer);
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
                listener.onPacket(packet);
            }
        }

        readBuffer.compact();
        if (!readBuffer.hasRemaining()) {
            connection.ensureReadBufferWritable(1024);
        }
    }

    private void writePackets(SelectionKey key) throws IOException {
        if (connection == null) {
            return;
        }

        connection.flushOutbound();
        if (!connection.hasOutboundData()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    private void handleDisconnect(boolean scheduleReconnect) {
        boolean wasConnected = connection != null;

        try {
            SelectionKey key = channel == null ? null : channel.keyFor(selector);
            if (key != null) {
                key.cancel();
            }
            if (channel != null) {
                channel.close();
            }
        } catch (IOException ignored) {
        }

        connection = null;
        channel = null;

        if (wasConnected) {
            listener.onDisconnected();
        }

        if (scheduleReconnect && running && reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++;
            reconnectAtMillis = System.currentTimeMillis() + (reconnectAttempts * baseReconnectDelayMillis);
        } else {
            reconnectAtMillis = -1L;
        }
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

    private void tryReconnectIfNeeded() throws IOException {
        if (!running || connection != null || reconnectAtMillis < 0L) {
            return;
        }

        if (System.currentTimeMillis() < reconnectAtMillis) {
            return;
        }

        reconnectAtMillis = -1L;
        openNewChannelAndConnect();
    }

    private void openNewChannelAndConnect() throws IOException {
        channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(host, port));
        channel.register(selector, SelectionKey.OP_CONNECT);
    }

    private void shutdownNow() {
        handleDisconnect(false);

        try {
            if (selector != null) {
                selector.close();
            }
        } catch (IOException ignored) {
        }
    }
}

