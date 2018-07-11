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

    public static final Comparator<Peer> BY_ID_ADDRESS = new Comparator<Peer>() {
        @Override
        public int compare(Peer o1, Peer o2) {

            return ComparisonChain.start()
                    .compare(o1.id, o2.id)
                    .compare(o1.address.getAddress(), o2.address.getAddress(), UnsignedBytes.lexicographicalComparator())
                    .result();
        }
    };

    public enum Status {
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
    public Date mFirstSeen;
    public Date mLastSeen;

    public Peer(ID id, InetAddress address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
        this.mFirstSeen = new Date();
        markSeen();
    }

    public void markSeen() {
        mLastSeen = new Date();
    }

    public Status getStatus() {
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
        return String.format("%s/%s (%s)", id.breifToString(), address, getStatus());
    }
}
