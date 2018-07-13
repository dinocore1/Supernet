package com.devsmart.supernet;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

class SupernetClientImp extends SupernetClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupernetClientImp.class);

    DatagramSocket mUDPSocket;

    private Thread mUDPReceiveThread;
    private boolean mUDPSocketRunning;
    private PacketReceiver mPacketReceiver;
    final ScheduledExecutorService mMainThread = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "SupernetClient Main Thread");
        }
    });

    RoutingTable mPeerRoutingTable;
    public final EventBus mEventBus = new EventBus();
    PeerMaintenenceTask mPeerMaintenence;
    SupernetClientProtocolReceiver mBaseProtocolReceiver;


    public void peerSeen(Peer peer) {
        if(!peer.id.equals(mClientId)) {
            RoutingTable.Bucket bucket = mPeerRoutingTable.getBucket(peer.id);
            bucket.addPeer(peer);
        }
    }

    @Override
    public void bootstrap(final String strAddress) {
        mMainThread.execute(new Runnable(){
            @Override
            public void run() {
                try {

                    InetSocketAddress address = Utils.parseSocketAddress(strAddress);

                    LOGGER.trace("sending ping to: {}", address);

                    DatagramPacket packet = SupernetClientProtocolReceiver.createPing(address, getID());
                    mUDPSocket.send(packet);

                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
        });

    }

    @Override
    public DatagramSocket getUDPSocket() {
        return mUDPSocket;
    }

    @Override
    public void start() {
        try {

            LOGGER.info("Local peer starting with ID: {}", mClientId);

            mAddresses.add(mUDPSocket.getLocalSocketAddress());

            mBaseProtocolReceiver = new SupernetClientProtocolReceiver();
            mBaseProtocolReceiver.mClient = this;
            mPacketReceiver = mBaseProtocolReceiver;

            mUDPReceiveThread = new Thread(mReceiveUDPTask, "Receive UDP");
            mUDPReceiveThread.start();

            mPeerMaintenence = new PeerMaintenenceTask(this);
            mPeerMaintenence.start();

        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            mPeerMaintenence.stop();
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

    public void setReceiver(PacketReceiver receiver) {
        mPacketReceiver = receiver;
    }

    public void post(Runnable r) {
        mMainThread.execute(r);
    }

    public void postEvent(final Object event) {
        post(new Runnable() {
            @Override
            public void run() {
                mEventBus.post(event);
            }
        });
    }

    private final Runnable mReceiveUDPTask = new Runnable() {

        private DatagramPacket createPacket() {
            final int packetSize = 64*1024;
            return new DatagramPacket(new byte[packetSize], packetSize);
        }

        @Override
        public void run() {
            try {
                LOGGER.info("starting UDP server on: {}", mUDPSocket.getLocalSocketAddress());
                mUDPSocketRunning = true;
                mUDPSocket.setSoTimeout(1000);

                while (mUDPSocketRunning) {
                    final DatagramPacket receivedPacket = createPacket();
                    try {
                        mUDPSocket.receive(receivedPacket);

                        LOGGER.trace("received packet {}", receivedPacket.getAddress());

                        post(new Runnable() {
                            @Override
                            public void run() {
                                if (mPacketReceiver != null) {
                                    mPacketReceiver.receive(receivedPacket);
                                } else {
                                    LOGGER.warn("dropped packet");
                                }
                            }
                        });

                    } catch (SocketTimeoutException e) {
                    }
                }

            }catch (IOException e) {
                LOGGER.error("", e);
            }

            LOGGER.info("exiting UDP receive thread");

        }
    };

}
