package me.tamkungz.nilloadersdk.network;

import me.tamkungz.nilloadersdk.network.packet.Packet;

public interface ClientListener {

    void onConnected();

    void onDisconnected();

    void onPacket(Packet packet);

    void onException(Throwable throwable);
}

