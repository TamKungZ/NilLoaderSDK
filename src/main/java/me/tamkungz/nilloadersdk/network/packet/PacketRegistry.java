package me.tamkungz.nilloadersdk.network.packet;

import java.util.HashMap;
import java.util.Map;

public final class PacketRegistry {

    private final Map<Integer, PacketFactory<? extends Packet>> idToFactory = new HashMap<Integer, PacketFactory<? extends Packet>>();
    private final Map<Class<? extends Packet>, Integer> classToId = new HashMap<Class<? extends Packet>, Integer>();

    public synchronized <T extends Packet> void register(int packetId, Class<T> packetClass, PacketFactory<T> factory) {
        if (idToFactory.containsKey(packetId)) {
            throw new IllegalArgumentException("Duplicate packet id: " + packetId);
        }
        if (classToId.containsKey(packetClass)) {
            throw new IllegalArgumentException("Duplicate packet class: " + packetClass.getName());
        }
        idToFactory.put(packetId, factory);
        classToId.put(packetClass, packetId);
    }

    public synchronized Packet create(int packetId) {
        PacketFactory<? extends Packet> factory = idToFactory.get(packetId);
        return factory == null ? null : factory.create();
    }

    public synchronized int resolvePacketId(Packet packet) {
        Integer packetId = classToId.get(packet.getClass());
        if (packetId == null) {
            throw new IllegalArgumentException("Unregistered packet class: " + packet.getClass().getName());
        }
        return packetId.intValue();
    }
}

