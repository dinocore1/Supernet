package com.devsmart.supernet;


import com.devsmart.supernet.events.NewPeerDiscovered;
import com.google.common.base.Throwables;
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

    public static final int HEADER_MAGIC_MASK = 0xF0;
    public static final int HEADER_MAGIC = 0x20;
    public static final int HEADER_PACKET_TYPE_MASK = 0x07;
    public static final int HEADER_REQUEST_BIT = 0x08;

    public static final int PACKET_PING = 0;
    public static final int PACKET_FIND_PEERS = 1;
    public static final int PACKET_ROUTE = 2;
    public static final int PACKET_CONNECT = 3;
    public static final int PACKET_DISCONNECT = 4;

    public static

    SupernetClientImp mClient;

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

                default:
                    LOGGER.warn("unknown packet type recevied");
                    break;
            }
        }


        return false;
    }

    public static DatagramPacket createPing(InetSocketAddress remoteAddress, ID id) throws SocketException {
        byte[] payload = new byte[1 + ID.NUM_BYTES];
        payload[0] = HEADER_MAGIC | PACKET_PING | HEADER_REQUEST_BIT;
        id.write(payload, 1);
        return new DatagramPacket(payload, payload.length, remoteAddress);
    }

    public static DatagramPacket createPong(InetSocketAddress remoteAddress, ID id) throws SocketException {
        Inet4Address ipv4Address = (Inet4Address) remoteAddress.getAddress();

        byte[] payload = new byte[1 + ID.NUM_BYTES + 6]; // header + ID + IPv4 SocketAddress
        payload[0] = HEADER_MAGIC | PACKET_PING;
        id.write(payload, 1);
        Utils.writeIPv4SocketAddress(ipv4Address, remoteAddress.getPort(), payload, 1 + ID.NUM_BYTES);

        return new DatagramPacket(payload, payload.length, remoteAddress);
    }

    public static DatagramPacket createFindPeersRequest(SocketAddress remoteAddress, ID target) throws SocketException {
        byte[] payload = new byte[1 + ID.NUM_BYTES];
        payload[0] = HEADER_MAGIC | PACKET_FIND_PEERS | HEADER_REQUEST_BIT;
        target.write(payload, 1);

        return new DatagramPacket(payload, payload.length, remoteAddress);
    }

    public static DatagramPacket createFindPeersResponse(SocketAddress remoteAddress, RoutingTable routingTable, ID targetPeer) throws SocketException {
        ArrayList<Peer> closestPeers = new ArrayList<Peer>(8);
        Iterator<Peer> nearByPeers = routingTable.getClosestPeers(targetPeer).iterator();
        for(int i=0;i<8 && nearByPeers.hasNext();i++) {
            closestPeers.add(nearByPeers.next());
        }

        byte[] payload = new byte[2 + (closestPeers.size() * (ID.NUM_BYTES + 6))];
        payload[0] = HEADER_MAGIC | PACKET_FIND_PEERS;
        payload[1] = (byte) closestPeers.size();

        int i=0;
        for(Peer p : closestPeers) {
            p.id.write(payload, 2 + i*(ID.NUM_BYTES + 6));
            InetSocketAddress address = p.getSocketAddress();

            Utils.writeIPv4SocketAddress((Inet4Address) address.getAddress(), address.getPort(),
                    payload, 2 + i*(ID.NUM_BYTES+6) + ID.NUM_BYTES);
            i++;
        }

        return new DatagramPacket(payload, payload.length, remoteAddress);
    }


    boolean receivePing(boolean isRequest, DatagramPacket packet) {
        try {
            final ID remoteId = new ID(packet.getData(), 1);
            Peer peer = mClient.mPeerRoutingTable.lookupPeer(new Peer(remoteId, packet.getAddress(), packet.getPort()));
            mClient.peerSeen(peer);

            boolean isPing = isRequest;
            LOGGER.trace("{} Received from: {}", isPing ? "Ping" : "Pong", peer);

            if(isRequest) {
                LOGGER.trace("sending pong to: {}", peer);
                mClient.mUDPSocket.send(createPong(peer.getSocketAddress(), mClient.getID()));
            }

            return true;
        } catch (IOException e) {
            LOGGER.error("", e);
            return false;
        }
    }

    private boolean receiveFindPeers(boolean isRequest, DatagramPacket packet) {
        try {
            final SocketAddress remoteAddress = packet.getSocketAddress();
            LOGGER.trace("FindPeers Received from: {}", remoteAddress);
            if (isRequest) {
                final ID targetPeer = new ID(packet.getData(), 1);
                mClient.mUDPSocket.send(createFindPeersResponse(remoteAddress, mClient.mPeerRoutingTable, targetPeer));
                return true;

            } else {
                byte[] payload = packet.getData();
                int size = payload[1];
                for(int i=0;i<size;i++) {
                    ID peerId = new ID(payload, 2 + i*(ID.NUM_BYTES+6));
                    if(!peerId.equals(mClient.mClientId)) {
                        InetSocketAddress peerSocketAddress = Utils.readIPv4SocketAddress(payload, 2 + i * (ID.NUM_BYTES + 6) + ID.NUM_BYTES);

                        Peer peer = mClient.mPeerRoutingTable.lookupPeer(new Peer(peerId, peerSocketAddress));

                        LOGGER.trace("discovered new peer: {} from: {}", peer, packet.getSocketAddress());

                        mClient.mPeerRoutingTable.addPeer(peer);
                    }
                }

                return true;
            }

        } catch (IOException e) {
            LOGGER.error("", e);
            return false;
        }

    }


}
