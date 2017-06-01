package com.devsmart.supernet;


import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public abstract class SupernetClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupernetClient.class);

    ID mClientId;

    SupernetClient() {}

    public ID getID() {
        return mClientId;
    }

    public static class Builder {

        private ID mId;
        private int mUDPPort = 11382;
        private SocketAddress mSTUNServer = new InetSocketAddress("stun.l.google.com", 19302);

        public Builder withId(ID id) {
            mId = id;
            return this;
        }

        public Builder withUDPPort(int port) {
            mUDPPort = port;
            return this;
        }

        public SupernetClient build() throws IOException {
            Preconditions.checkState(mId != null);

            SupernetClientImp retval = new SupernetClientImp();
            retval.mClientId = mId;
            retval.mPeerRoutingTable = new RoutingTable(mId);
            retval.mSTUNServer = mSTUNServer;
            retval.mUDPSocket = new DatagramSocket(new InetSocketAddress(InetAddresses.forString("0.0.0.0"), mUDPPort));
            retval.mUDPSocket.setReuseAddress(true);
            retval.mUDPSocket.setSoTimeout(1000);
            return retval;
        }
    }

    public abstract void start();
    public abstract void shutdown();


}
