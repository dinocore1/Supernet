package com.devsmart.supernet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

class SupernetClientImp extends SupernetClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupernetClientImp.class);

    SocketAddress mSTUNServer;
    DatagramSocket mUDPSocket;
    final ArrayList<SocketAddress> mAddresses = new ArrayList<SocketAddress>();

    private Thread mUDPReceiveThread;
    private boolean mUDPSocketRunning;
    private STUNBindingRequest mSTUNBindingRequest;
    private ArrayList<PacketReceiver> mPacketReceivers = new ArrayList<PacketReceiver>();
    final ScheduledExecutorService mMainThread = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "SupernetClient Main Thread");
        }
    });

    RoutingTable mPeerRoutingTable;


    public void peerSeen(SocketAddress remoteAddress, ID remoteId) {

    }

    @Override
    public void start() {
        try {
            mAddresses.clear();
            mAddresses.add(mUDPSocket.getLocalSocketAddress());


            mUDPReceiveThread = new Thread(mReceiveUDPTask, "Receive UDP");
            mUDPReceiveThread.start();

            doSTUNBindingRequest();

        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (mUDPReceiveThread != null) {
                mUDPSocketRunning = false;
                mUDPReceiveThread.join();
                mUDPReceiveThread = null;
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        LOGGER.info("Client shutdown");

    }

    private void doSTUNBindingRequest() throws Exception {
        STUNBindingRequest bindingRequest = new STUNBindingRequest(this, mSTUNServer);
        registerReceiver(bindingRequest);
        bindingRequest.doIt();
    }

    public void registerReceiver(PacketReceiver receiver) {
        mPacketReceivers.add(receiver);
    }

    public void unregisterReceiver(PacketReceiver receiver) {
        mPacketReceivers.remove(receiver);
    }

    public void post(Runnable r) {
        mMainThread.execute(r);
    }

    private final Runnable mReceiveUDPTask = new Runnable() {

        private DatagramPacket createPacket() {
            final int packetSize = 64*1024;
            return new DatagramPacket(new byte[packetSize], packetSize);
        }

        @Override
        public void run() {
            LOGGER.info("starting UDP server on: {}", mUDPSocket.getLocalSocketAddress());
            mUDPSocketRunning = true;
            while(mUDPSocketRunning) {
                final DatagramPacket receivedPacket = createPacket();
                try {
                    mUDPSocket.receive(receivedPacket);

                    post(new Runnable() {
                        @Override
                        public void run() {
                            for(PacketReceiver r : mPacketReceivers) {
                                if(r.receive(receivedPacket)) {
                                    break;
                                }
                            }
                        }
                    });

                } catch (SocketTimeoutException e) {
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
            LOGGER.info("exiting UDP receive thread");

        }
    };

}