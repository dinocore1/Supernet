package com.devsmart.supernet;


import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.UnmodifiableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PeerMaintenenceTask {

    public static final Logger LOGGER = LoggerFactory.getLogger(PeerMaintenenceTask.class);

    private final SupernetClientImp mClient;
    private ScheduledFuture<?> mFindPeersTask;
    private ScheduledFuture<?> mKeepAliveTask;
    private final RoutingTable.Bucket[] mBucketList;


    public PeerMaintenenceTask(SupernetClientImp client) {
        mClient = client;
        mBucketList = mClient.mPeerRoutingTable.mBuckets;
    }

    public void start() {
        if (mFindPeersTask != null) {
            mFindPeersTask.cancel(false);
        }

        mFindPeersTask = mClient.mMainThread.scheduleWithFixedDelay(mFindPeersFunction, 10, 40, TimeUnit.SECONDS);
        mKeepAliveTask = mClient.mMainThread.scheduleWithFixedDelay(mKeepAliveFunction, 10, 5, TimeUnit.SECONDS);

    }

    public void stop() {
        if (mFindPeersTask != null) {
            mFindPeersTask.cancel(false);
            mFindPeersTask = null;
        }

        if (mKeepAliveTask != null) {
            mKeepAliveTask.cancel(false);
            mKeepAliveTask = null;
        }
    }

    private final Runnable mFindPeersFunction = new Runnable() {

        private Random mRandom = new Random();

        @Override
        public void run() {

            //Collections.shuffle(mBucketList);
            for (RoutingTable.Bucket b : mBucketList) {
                ImmutableSortedSet<Peer> peers = b.getOldestPeers();

                if (!peers.isEmpty()) {
                    int r = mRandom.nextInt(peers.size());
                    Peer p = Iterables.get(peers, r);
                    sendFindPeers(p);
                    return;
                }
            }
        }

    };

    private final Runnable mKeepAliveFunction = new Runnable() {

        private Random mRandom = new Random();

        @Override
        public void run() {
            try {
                for (RoutingTable.Bucket b : mBucketList) {
                    ImmutableSortedSet<Peer> peers = b.getOldestPeers();
                    UnmodifiableIterator<Peer> it = peers.iterator();
                    int i = 0;
                    while (it.hasNext() && i < RoutingTable.MAX_BUCKET_SIZE) {
                        Peer p = it.next();
                        long randomDelay = mRandom.nextInt(300);
                        mClient.mMainThread.schedule(createPingFuture(p), randomDelay, TimeUnit.MILLISECONDS);
                        i++;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }

        private Runnable createPingFuture(final Peer p) {
            return new Runnable() {
                @Override
                public void run() {
                    sendPing(p);
                }
            };
        }
    };

    private void sendFindPeers(Peer p) {
        try {
            LOGGER.trace("sending find peers to: {}", p);

            DatagramPacket packet = SupernetClientProtocolReceiver.createFindPeersRequest(p.getSocketAddress(), mClient.mClientId);
            mClient.mUDPSocket.send(packet);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    private void sendPing(Peer p) {
        try {
            LOGGER.trace("sending ping to: {}", p);

            DatagramPacket packet = SupernetClientProtocolReceiver.createPing(p.getSocketAddress(), mClient.mClientId);
            mClient.mUDPSocket.send(packet);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }


}
