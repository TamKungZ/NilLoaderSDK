package me.tamkungz.nilkit.network.packet;

import java.util.HashMap;
import java.util.Map;

public final class PacketRegistry {

    private final Map<Integer, PacketFactory<? extends Packet>> idToFactory = new HashMap<Integer, PacketFactory<? extends Packet>>();
    private final Map<Class<? extends Packet>, Integer> classToId = new HashMap<Class<? extends Packet>, Integer>();

    public synchronized <T extends Packet> void register(int packetId, Class<T> packetClass, PacketFactory<T> factory) {
        if (packetId < 0) {
            throw new IllegalArgumentException("packetId must be >= 0");
        }
        if (packetClass == null) {
            throw new IllegalArgumentException("packetClass must not be null");
        }
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        if (idToFactory.containsKey(packetId)) {
            throw new IllegalArgumentException("Duplicate packet id: " + packetId);
        }
        if (classToId.containsKey(packetClass)) {
            throw new IllegalArgumentException("Duplicate packet class: " + packetClass.getName());
        }
        idToFactory.put(Integer.valueOf(packetId), factory);
        classToId.put(packetClass, Integer.valueOf(packetId));
    }

    public synchronized Packet create(int packetId) {
        PacketFactory<? extends Packet> factory = idToFactory.get(Integer.valueOf(packetId));
        return factory == null ? null : factory.create();
    }

    public synchronized int resolvePacketId(Packet packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }
        Integer packetId = classToId.get(packet.getClass());
        if (packetId == null) {
            throw new IllegalArgumentException("Unregistered packet class: " + packet.getClass().getName());
        }
        return packetId.intValue();
    }

    public synchronized boolean isRegistered(int packetId) {
        return idToFactory.containsKey(Integer.valueOf(packetId));
    }

    public synchronized boolean isRegistered(Class<? extends Packet> packetClass) {
        return packetClass != null && classToId.containsKey(packetClass);
    }

    public synchronized int size() {
        return idToFactory.size();
    }

    public synchronized void clear() {
        idToFactory.clear();
        classToId.clear();
    }
}
