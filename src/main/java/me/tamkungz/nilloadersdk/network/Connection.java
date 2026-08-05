package me.tamkungz.nilloadersdk.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

public final class Connection {

    private final SocketChannel channel;
    private ByteBuffer readBuffer;
    private final Queue<ByteBuffer> outboundQueue;

    public Connection(SocketChannel channel) {
        this.channel = channel;
        this.readBuffer = ByteBuffer.allocate(4096);
        this.outboundQueue = new ArrayDeque<ByteBuffer>();
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public void ensureReadBufferWritable(int minWritableBytes) {
        if (readBuffer.remaining() >= minWritableBytes) {
            return;
        }

        int required = readBuffer.position() + minWritableBytes;
        int newCapacity = readBuffer.capacity();
        while (newCapacity < required) {
            newCapacity = newCapacity * 2;
        }

        ByteBuffer expanded = ByteBuffer.allocate(newCapacity);
        readBuffer.flip();
        expanded.put(readBuffer);
        readBuffer = expanded;
    }

    public void enqueue(ByteBuffer buffer) {
        outboundQueue.add(buffer);
    }

    public boolean hasOutboundData() {
        return !outboundQueue.isEmpty();
    }

    public void flushOutbound() throws IOException {
        while (!outboundQueue.isEmpty()) {
            ByteBuffer head = outboundQueue.peek();
            channel.write(head);
            if (head.hasRemaining()) {
                return;
            }
            outboundQueue.poll();
        }
    }
}

