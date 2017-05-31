package com.devsmart.supernet;

import java.net.DatagramPacket;

public interface PacketReceiver {

    /**
     * @param packet
     * @return true if the the packet was handled by this function
     */
    boolean receive(DatagramPacket packet);
}