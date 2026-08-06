package me.tamkungz.nilkit.network.packet;

public interface PacketFactory<T extends Packet> {

    T create();
}

