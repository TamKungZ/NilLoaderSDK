package me.tamkungz.nilkit.network;

import me.tamkungz.nilkit.network.packet.Packet;

public interface ClientListener {

    void onConnected();

    void onDisconnected();

    void onPacket(Packet packet);

    void onException(Throwable throwable);

    /** Called when a complete frame uses an id that is not registered locally. */
    default void onUnknownPacket(int packetId) {
    }

    /** Called after all configured reconnect attempts have failed. */
    default void onReconnectExhausted(int attempts) {
    }
}
