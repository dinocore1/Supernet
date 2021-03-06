package com.devsmart.supernet;


import com.google.common.collect.ComparisonChain;
import com.google.common.primitives.UnsignedBytes;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Comparator;
import java.util.Date;

public class Peer {

    public static final Comparator<Peer> OLDEST_ALIVE_FIRST = new Comparator<Peer>() {

        @Override
        public int compare(Peer o1, Peer o2) {
            return ComparisonChain.start()
                    .compare(o1.getStatus(), o2.getStatus())
                    .compare(o1.mFirstSeen, o2.mFirstSeen)
                    .result();
        }
    };

    public static final Comparator<Peer> BY_ID = new Comparator<Peer>() {
        @Override
        public int compare(Peer o1, Peer o2) {

            return ComparisonChain.start()
                    .compare(o1.id, o2.id)
                    .compare(o1.address.getAddress(), o2.address.getAddress(), UnsignedBytes.lexicographicalComparator())
                    .compare(o1.port, o2.port)
                    .result();
        }
    };

    public enum Status {
        UNKNOWN(-1),
        ALIVE(10000),
        DIEING(30000),
        DEAD(90000);

        public final long threshold;

        Status(long threshold) {
            this.threshold = threshold;
        }
    }

    public final ID id;
    public final InetAddress address;
    public final int port;
    private final Date mFirstSeen;
    private Date mLastSeen;

    public Peer(ID id, InetAddress address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
        this.mFirstSeen = new Date();
    }

    public Peer(ID peerId, InetSocketAddress peerSocketAddress) {
        this.id = peerId;
        this.address = peerSocketAddress.getAddress();
        this.port = peerSocketAddress.getPort();
        this.mFirstSeen = new Date();
    }

    public void markSeen() {
        mLastSeen = new Date();
    }

    public Status getStatus() {
        if(mLastSeen == null) {
            return Status.UNKNOWN;
        }

        Date now = new Date();
        long milliSec = now.getTime() - mLastSeen.getTime();

        if(milliSec < Status.ALIVE.threshold) {
            return Status.ALIVE;
        } else if(milliSec < Status.DIEING.threshold) {
            return Status.DIEING;
        } else {
            return Status.DEAD;
        }
    }

    public InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(address, port);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || obj.getClass() != Peer.class) {
            return false;
        }

        Peer other = (Peer) obj;
        return id.equals(other.id) && address.equals(other.address) && port == other.port;
    }

    @Override
    public String toString() {
        return String.format("[%s|%s:%d (%s)]", id.breifToString(), address, port, getStatus());
    }
}
