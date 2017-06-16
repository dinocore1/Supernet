package com.devsmart.supernet;


import com.devsmart.supernet.events.NewPeerDiscovered;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
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

        mClient.mEventBus.register(this);
    }

    public void stop() {
        if(mFindPeersTask != null) {
            mFindPeersTask.cancel(false);
            mFindPeersTask = null;
        }

        mClient.mEventBus.unregister(this);
    }

    @Subscribe
    public void onNewPeerDiscovered(NewPeerDiscovered e) {
        RoutingTable.Bucket bucket = mClient.mPeerRoutingTable.getBucket(e.remoteId);
        if(bucket.peers.size() < RoutingTable.MAX_BUCKET_SIZE) {
            StartConnectionTask newTask = new StartConnectionTask(e.remoteId, e.socketAddress, e.gossipPeer);
            newTask.start();
        }

    }

    private class StartConnectionTask {

        private final ID mRemoteId;
        private final SocketAddress mRemoteAddress;
        private final SocketAddress mGossipPeer;

        public StartConnectionTask(ID remoteId, SocketAddress remoteAddress, SocketAddress gossipPeer) {
            mRemoteId = remoteId;
            mRemoteAddress = remoteAddress;
            mGossipPeer = gossipPeer;
        }


        public void start() {
            
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
