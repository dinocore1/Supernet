package com.devsmart.supernet;


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.UnmodifiableIterator;

import java.util.*;

public class RoutingTable {

    public static final int MAX_BUCKET_SIZE = 2;

    private final ID mLocalId;

    public class Bucket {
        final int sharedPrefixBits;
        private final TreeSet<Peer> peers = new TreeSet<Peer>(Peer.BY_ID);

        public Bucket(int sharedPrefixBits) {
            this.sharedPrefixBits = sharedPrefixBits;
        }

        public synchronized void addPeer(Peer p) {
            Preconditions.checkArgument(p.id.getNumSharedPrefixBits(mLocalId) == sharedPrefixBits);
            Peer existingPeer = peers.floor(p);
            if(existingPeer != null) {
                existingPeer.markSeen();
            } else {
                peers.add(p);
            }
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
        for(int i = 0;i<mBuckets.length;i++) {
            mBuckets[i] = new Bucket(i);
        }
    }

    public synchronized void addPeer(Peer p) {
        Bucket b = getBucket(p.id);
        b.addPeer(p);
    }

    public synchronized Bucket getBucket(ID id) {
        int index = mLocalId.getNumSharedPrefixBits(id);
        index = Math.min(index, (ID.NUM_BYTES*8)-1);
        return mBuckets[index];
    }

    public synchronized Peer lookupPeer(Peer peer) {
        Bucket b = getBucket(peer.id);
        Peer retval = b.peers.floor(peer);
        if(retval != null) {
            return retval;
        } else {
            return peer;
        }
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



}
