package me.tamkungz.nilloadersdk.network.packet;

public interface PacketFactory<T extends Packet> {

    T create();
}

