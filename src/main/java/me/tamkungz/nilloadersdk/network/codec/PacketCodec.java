package me.tamkungz.nilloadersdk.network.codec;

import me.tamkungz.nilloadersdk.network.packet.Packet;
import me.tamkungz.nilloadersdk.network.packet.PacketRegistry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Frame format: [length:int][packetId:int][payload...]
 * length includes packetId + payload.
 */
public final class PacketCodec {

    public static final int HEADER_SIZE = 4;
    public static final int PACKET_ID_SIZE = 4;

    private PacketCodec() {
    }

    public static ByteBuffer encode(PacketRegistry registry, Packet packet) throws IOException {
        int packetId = registry.resolvePacketId(packet);
        byte[] payload = serializePayload(packet);
        int payloadSize = payload.length;
        int frameSize = PACKET_ID_SIZE + payloadSize;

        ByteBuffer out = ByteBuffer.allocate(HEADER_SIZE + frameSize);
        out.putInt(frameSize);
        out.putInt(packetId);
        out.put(payload);
        out.flip();
        return out;
    }

    public static DecodedPacket tryDecode(PacketRegistry registry, ByteBuffer readBuffer, int maxFrameSize) throws IOException {
        if (readBuffer.remaining() < HEADER_SIZE) {
            return null;
        }

        readBuffer.mark();
        int frameLength = readBuffer.getInt();
        if (frameLength < PACKET_ID_SIZE || frameLength > maxFrameSize) {
            throw new IOException("Invalid frame length: " + frameLength);
        }

        if (readBuffer.remaining() < frameLength) {
            readBuffer.reset();
            return null;
        }

        int packetId = readBuffer.getInt();
        int payloadLength = frameLength - PACKET_ID_SIZE;

        byte[] payload = new byte[payloadLength];
        readBuffer.get(payload);

        Packet packet = registry.create(packetId);
        if (packet != null) {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
            packet.read(in);
        }

        return new DecodedPacket(packetId, packet);
    }

    private static byte[] serializePayload(Packet packet) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
        DataOutputStream out = new DataOutputStream(baos);
        packet.write(out);
        out.flush();
        return baos.toByteArray();
    }

    public static final class DecodedPacket {
        private final int packetId;
        private final Packet packet;

        public DecodedPacket(int packetId, Packet packet) {
            this.packetId = packetId;
            this.packet = packet;
        }

        public int getPacketId() {
            return packetId;
        }

        public Packet getPacket() {
            return packet;
        }
    }
}

