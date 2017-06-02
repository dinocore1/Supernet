package com.devsmart.supernet;


import com.devsmart.ubjson.UBArray;
import com.devsmart.ubjson.UBValue;
import com.devsmart.ubjson.UBValueFactory;
import com.devsmart.ubjson.UBWriter;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;

public class SupernetClientProtocolReceiver implements PacketReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupernetClientProtocolReceiver.class);

    /*
    Header byte definition:
    7:4 reserved/magic = 0
    4: isRequest
    3:0 packet type

    Packet Types:
    0: ping
    1: find peers
    2: route
    3: connect
    4: disconnect


    Ping Request:
    Payload: requester's ID

    Ping Response:
    Payload: responder's ID + requester's IPv4 address

    Find Peers Request:
    Payload: ID of target

    Find Peers Response:
    Payload: UBJSON Array list of ID+IPv4 Addresses closest to target

    Route Request:
    Payload: uint8 hops + uint16 packet size + data payload

    Route Response:
    OK/ERROR

    Connect Request:
    Payload: ID + port

    Connect Response:
    OK/ERROR

    Disconnect Request:
    Payload: ID + port

    Disconnect Response:
    OK/ERROR
    */

    private static final int HEADER_MAGIC_MASK = 0xF0;
    private static final int HEADER_MAGIC = 0x20;
    private static final int HEADER_PACKET_TYPE_MASK = 0x07;
    private static final int HEADER_REQUEST_BIT = 0x08;

    private static final int PACKET_PING = 0;
    private static final int PACKET_FIND_PEERS = 1;
    private static final int PACKET_ROUTE = 2;
    private static final int PACKET_CONNECT = 3;
    private static final int PACKET_DISCONNECT = 4;

    private SupernetClientImp mClient;

    @Override
    public boolean receive(DatagramPacket packet) {

        byte[] data = packet.getData();
        byte header = data[0];

        if((header & HEADER_MAGIC_MASK) == HEADER_MAGIC) {
            final int packetType = (header & HEADER_PACKET_TYPE_MASK);
            final boolean isRequest = (header & HEADER_REQUEST_BIT) > 0;

            switch(packetType) {
                case PACKET_PING:
                    return receivePing(isRequest, packet);

                case PACKET_FIND_PEERS:
                    return receiveFindPeers(isRequest, packet);

                case PACKET_ROUTE:
                    break;

                case PACKET_CONNECT:
                    break;

                case PACKET_DISCONNECT:
                    break;
            }
        }


        return false;
    }

    static void writeIPv4SocketAddress(Inet4Address socketAddress, int port, byte[] buf, int offset) {

        byte[] address = socketAddress.getAddress();

        System.arraycopy(address, 0, buf, offset, 4);


        buf[offset + 4] = (byte) ((port >>> 8) & 0xFF);
        buf[offset + 5] = (byte) (port & 0xFF);
    }

    static InetSocketAddress readIPv4SocketAddress(byte[] buf, int offset) {
        try {
            byte[] addressBytes = new byte[4];
            System.arraycopy(buf, offset, addressBytes, 0, 4);
            InetAddress address = Inet4Address.getByAddress(addressBytes);
            int port = (0xFF & buf[offset + 4]) << 8;
            port |= (0xFF & buf[offset + 5]);

            return new InetSocketAddress(address, port);
        } catch (UnknownHostException e) {
            LOGGER.error("", e);
            Throwables.propagate(e);
            return null;
        }
    }

    static DatagramPacket createPong(InetSocketAddress remoteAddress, ID id) {
        Inet4Address ipv4Address = (Inet4Address) remoteAddress.getAddress();

        byte[] payload = new byte[1 + ID.NUM_BYTES + 6]; // header + ID + IPv4 SocketAddress
        payload[0] = HEADER_MAGIC | PACKET_PING;
        id.write(payload, 1);
        writeIPv4SocketAddress(ipv4Address, remoteAddress.getPort(), payload, 1 + ID.NUM_BYTES);

        return new DatagramPacket(payload, payload.length, remoteAddress);
    }


    boolean receivePing(boolean isRequest, DatagramPacket packet) {
        try {
            final SocketAddress remoteAddress = packet.getSocketAddress();

            ID remoteId = new ID(packet.getData(), 1);
            LOGGER.trace("Ping Received from: {}:{}", remoteAddress, remoteId);

            mClient.peerSeen(remoteAddress, remoteId);

            if(isRequest) {
                mClient.mUDPSocket.send(createPong((InetSocketAddress) remoteAddress, mClient.getID()));
            }

            return true;
        } catch (IOException e) {
            LOGGER.error("", e);
            return false;
        }
    }

    byte[] createClosestPeersResponse(ID target) {
        ArrayList<Peer> closestPeers = new ArrayList<Peer>(8);
        Iterator<Peer> nearByPeers = mClient.mPeerRoutingTable.getClosestPeers(target).iterator();
        for(int i=0;i<8 && nearByPeers.hasNext();i++) {
            closestPeers.add(nearByPeers.next());
        }

        byte[] retval = new byte[1 + (closestPeers.size() * (ID.NUM_BYTES + 6))];
        retval[0] = HEADER_MAGIC | PACKET_FIND_PEERS;
        retval[1] = (byte) closestPeers.size();

        int i=0;
        for(Peer p : closestPeers) {
            p.id.write(retval, 2 + i*(ID.NUM_BYTES + 6));
            InetSocketAddress address = (InetSocketAddress) p.address;

            writeIPv4SocketAddress((Inet4Address) address.getAddress(), address.getPort(),
                    retval, 2 + i*(ID.NUM_BYTES+6) + ID.NUM_BYTES);
            i++;
        }
        return retval;
    }

    private boolean receiveFindPeers(boolean isRequest, DatagramPacket packet) {
        try {
            if (isRequest) {
                final ID targetPeer = new ID(packet.getData(), 1);
                byte[] payload = createClosestPeersResponse(targetPeer);
                DatagramPacket responsePacket = new DatagramPacket(payload, payload.length, packet.getSocketAddress());
                mClient.mUDPSocket.send(responsePacket);
                return true;

            } else {
                byte[] payload = packet.getData();
                int size = payload[1];
                for(int i=0;i<size;i++) {
                    ID peerId = new ID(payload, 2 + i*(ID.NUM_BYTES+6));
                    InetSocketAddress socketAddress = readIPv4SocketAddress(payload, 2 + i * (ID.NUM_BYTES + 6) + ID.NUM_BYTES);
                    discoveredNewPeer(packet.getAddress(), peerId, socketAddress);
                }

                return true;
            }

        } catch (IOException e) {
            LOGGER.error("", e);
            return false;
        }

    }

    private void discoveredNewPeer(InetAddress gossipAddress, ID peerId, InetSocketAddress peerSocketAddress) {
        LOGGER.trace("discovered new peer: {}:{} from: {}", peerId, peerSocketAddress, gossipAddress);


    }


}