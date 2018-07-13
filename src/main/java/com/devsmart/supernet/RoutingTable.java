package com.devsmart.supernet;


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.UnmodifiableIterator;

import java.util.*;

public class RoutingTable {

    private static final Comparator<Peer> OLDEST_PEER_COMPARATOR = new Comparator<Peer>() {

        @Override
        public int compare(Peer o1, Peer o2) {
            return o1.mFirstSeen.compareTo(o2.mFirstSeen);
        }
    };

    public static final int MAX_BUCKET_SIZE = 8;

    private final ID mLocalId;

    public class Bucket {
        final int sharedPrefixBits;
        private final TreeSet<Peer> peers = new TreeSet<Peer>(Peer.BY_ID);

        public Bucket(int sharedPrefixBits) {
            this.sharedPrefixBits = sharedPrefixBits;
        }

        public synchronized void addPeer(Peer p) {
            Preconditions.checkArgument(p.id.getNumSharedPrefixBits(mLocalId) == sharedPrefixBits);
            peers.add(p);
        }

        public synchronized ImmutableSortedSet<Peer> getOldestPeers() {
            return ImmutableSortedSet.orderedBy(Peer.OLDEST_ALIVE_FIRST)
                    .addAll(peers)
                    .build();
        }

        public synchronized void trimPeers() {

            ImmutableSortedSet<Peer> oldestFirst = getOldestPeers();
            UnmodifiableIterator<Peer> it = oldestFirst.descendingIterator();

            while(it.hasNext() && peers.size() > MAX_BUCKET_SIZE) {
                Peer peerToRemove = it.next();
                peers.remove(peerToRemove);
            }
        }
    }

    Bucket[] mBuckets = new Bucket[ID.NUM_BYTES * 8];

    public RoutingTable(ID localId) {
        mLocalId = localId;
    }

    public synchronized Bucket getBucket(ID id) {
        final int index = mLocalId.getNumSharedPrefixBits(id);
        Bucket retval = mBuckets[index];
        if(retval == null) {
            retval = new Bucket(index);
            mBuckets[index] = retval;
        }
        return retval;
    }

    public Iterable<Peer> getClosestPeers(ID id) {

        ArrayList<Iterable<Peer>> iterators = new ArrayList<Iterable<Peer>>(30);

        for(int i = mLocalId.getNumSharedPrefixBits(id); i>=0;i--) {
            Bucket bucket = mBuckets[i];
            if(bucket != null) {
                iterators.add(bucket.peers);
            }
        }

        return Iterables.concat(iterators);

    }


    void evictPeer(Bucket bucket, Peer peer) {

    }




}
