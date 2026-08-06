package me.tamkungz.nilkit.network;

import me.tamkungz.nilkit.network.packet.Packet;

public interface ServerListener {

    void onConnected(Connection connection);

    void onDisconnected(Connection connection);

    void onPacket(Connection connection, Packet packet);

    void onException(Connection connection, Throwable throwable);

    /** Called when a complete frame uses an id that is not registered locally. */
    default void onUnknownPacket(Connection connection, int packetId) {
    }
}
