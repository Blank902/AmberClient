package com.amberclient.events.network;

import net.minecraft.network.packet.Packet;

public interface PacketReceiveListener {
    void onPacketReceive(Packet<?> packet);
}