package me.tamkungz.nilloadersdk.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class Connection {

    private final SocketChannel channel;
    private ByteBuffer readBuffer;
    private final Queue<ByteBuffer> outboundQueue;

    public Connection(SocketChannel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }
        this.channel = channel;
        this.readBuffer = ByteBuffer.allocate(4096);
        this.outboundQueue = new ConcurrentLinkedQueue<ByteBuffer>();
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public void ensureReadBufferWritable(int minWritableBytes) {
        if (minWritableBytes < 0) {
            throw new IllegalArgumentException("minWritableBytes must be >= 0");
        }
        if (readBuffer.remaining() >= minWritableBytes) {
            return;
        }

        int required = readBuffer.position() + minWritableBytes;
        int newCapacity = readBuffer.capacity();
        while (newCapacity < required) {
            if (newCapacity > Integer.MAX_VALUE / 2) {
                newCapacity = required;
                break;
            }
            newCapacity = newCapacity * 2;
        }

        ByteBuffer expanded = ByteBuffer.allocate(newCapacity);
        readBuffer.flip();
        expanded.put(readBuffer);
        readBuffer = expanded;
    }

    public void enqueue(ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        outboundQueue.add(buffer);
    }

    public boolean hasOutboundData() {
        return !outboundQueue.isEmpty();
    }

    public int getPendingWriteCount() {
        return outboundQueue.size();
    }

    public void flushOutbound() throws IOException {
        while (true) {
            ByteBuffer head = outboundQueue.peek();
            if (head == null) {
                return;
            }
            channel.write(head);
            if (head.hasRemaining()) {
                return;
            }
            outboundQueue.poll();
        }
    }
}
