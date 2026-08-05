package me.tamkungz.nilloadersdk.network;

import me.tamkungz.nilloadersdk.network.packet.Packet;

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
