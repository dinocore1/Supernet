package com.devsmart.supernet;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PeerMaintenenceTask {

    public static final Logger LOGGER = LoggerFactory.getLogger(PeerMaintenenceTask.class);

    private final SupernetClientImp mClient;
    private ScheduledFuture<?> mFindPeersTask;
    private final ArrayList<RoutingTable.Bucket> mBucketList;

    public PeerMaintenenceTask(SupernetClientImp client) {
        mClient = client;

        mBucketList = new ArrayList<RoutingTable.Bucket>(Arrays.asList(mClient.mPeerRoutingTable.mBuckets));
    }

    public void start() {
        if(mFindPeersTask != null) {
            mFindPeersTask.cancel(false);
        }
        mFindPeersTask = mClient.mMainThread.scheduleWithFixedDelay(mFindPeersFunction, 10, 40, TimeUnit.SECONDS);
    }

    public void stop() {
        if(mFindPeersTask != null) {
            mFindPeersTask.cancel(false);
            mFindPeersTask = null;
        }
    }

    private final Runnable mFindPeersFunction = new Runnable() {
        @Override
        public void run() {
            Collections.shuffle(mBucketList);
            for(RoutingTable.Bucket b : mBucketList) {
                ArrayList<Peer> peers = new ArrayList<Peer>(b.peers);
                peers.sort(Peer.OLDEST_ALIVE_FIRST);
                if(!peers.isEmpty()){
                    sendFindPeers(peers.get(0));
                    return;
                }
            }

        }
    };

    private void sendFindPeers(Peer p) {
        try {
            LOGGER.trace("sending find peers to: {}", p);

            byte[] payload = new byte[1 + ID.NUM_BYTES];
            payload[0] = SupernetClientProtocolReceiver.HEADER_MAGIC
                    | SupernetClientProtocolReceiver.HEADER_REQUEST_BIT
                    | SupernetClientProtocolReceiver.PACKET_FIND_PEERS;

            mClient.getID().write(payload, 1);

            DatagramPacket packet = new DatagramPacket(payload, payload.length, p.address);
            mClient.mUDPSocket.send(packet);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }


}
