package me.tamkungz.nilloadersdk.network.example;

import me.tamkungz.nilloadersdk.network.packet.Packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class PingPacket implements Packet {

    private long sentAtMillis;

    public PingPacket() {
    }

    public PingPacket(long sentAtMillis) {
        this.sentAtMillis = sentAtMillis;
    }

    public long getSentAtMillis() {
        return sentAtMillis;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(sentAtMillis);
    }

    @Override
    public void read(DataInput in) throws IOException {
        sentAtMillis = in.readLong();
    }

    @Override
    public String toString() {
        return "PingPacket{sentAtMillis=" + sentAtMillis + '}';
    }
}

