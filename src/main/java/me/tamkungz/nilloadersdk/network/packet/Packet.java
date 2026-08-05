package me.tamkungz.nilloadersdk.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Variable-length packet contract for the simple NIO protocol.
 */
public interface Packet {

    void write(DataOutput out) throws IOException;

    void read(DataInput in) throws IOException;
}

