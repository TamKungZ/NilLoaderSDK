package me.tamkungz.nilloadersdk.network;

import me.tamkungz.nilloadersdk.network.packet.Packet;

public interface ServerListener {

    void onConnected(Connection connection);

    void onDisconnected(Connection connection);

    void onPacket(Connection connection, Packet packet);

    void onException(Connection connection, Throwable throwable);
}

