package me.tamkungz.nilkit.network.codec;

import me.tamkungz.nilkit.network.packet.Packet;
import me.tamkungz.nilkit.network.packet.PacketFactory;
import me.tamkungz.nilkit.network.packet.PacketRegistry;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class PacketCodecTest {

    @Test
    public void roundTripsPacketAndSupportsPartialFrames() throws Exception {
        PacketRegistry registry = registry();
        ByteBuffer encoded = PacketCodec.encode(registry, new IntPacket(42));

        ByteBuffer partial = ByteBuffer.allocate(encoded.remaining());
        for (int i = 0; i < 5; i++) partial.put(encoded.get());
        partial.flip();
        assertNull(PacketCodec.tryDecode(registry, partial, 1024));
        assertEquals(0, partial.position());

        ByteBuffer full = PacketCodec.encode(registry, new IntPacket(42));
        PacketCodec.DecodedPacket decoded = PacketCodec.tryDecode(registry, full, 1024);
        assertNotNull(decoded);
        assertEquals(7, decoded.getPacketId());
        assertEquals(42, ((IntPacket) decoded.getPacket()).value);
    }

    @Test
    public void unknownPacketStillConsumesItsFrame() throws Exception {
        PacketRegistry sender = registry();
        PacketRegistry receiver = new PacketRegistry();
        ByteBuffer encoded = PacketCodec.encode(sender, new IntPacket(9));

        PacketCodec.DecodedPacket decoded = PacketCodec.tryDecode(receiver, encoded, 1024);
        assertNotNull(decoded);
        assertEquals(7, decoded.getPacketId());
        assertNull(decoded.getPacket());
        assertFalse(encoded.hasRemaining());
    }

    @Test(expected = IOException.class)
    public void rejectsOversizedFrame() throws Exception {
        PacketRegistry registry = registry();
        ByteBuffer bad = ByteBuffer.allocate(8).putInt(999).putInt(7);
        bad.flip();
        PacketCodec.tryDecode(registry, bad, 32);
    }

    private static PacketRegistry registry() {
        PacketRegistry registry = new PacketRegistry();
        registry.register(7, IntPacket.class, new PacketFactory<IntPacket>() {
            @Override
            public IntPacket create() {
                return new IntPacket();
            }
        });
        return registry;
    }

    private static final class IntPacket implements Packet {
        int value;

        IntPacket() {
        }

        IntPacket(int value) {
            this.value = value;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeInt(value);
        }

        @Override
        public void read(DataInput in) throws IOException {
            value = in.readInt();
        }
    }
}
