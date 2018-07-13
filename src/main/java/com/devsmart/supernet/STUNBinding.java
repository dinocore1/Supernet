package com.devsmart.supernet;

import com.google.common.collect.Iterables;
import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.ChangedAddress;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.header.MessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public class STUNBinding implements Callable<InetSocketAddress> {

    private static final Logger LOGGER = LoggerFactory.getLogger(STUNBinding.class);

    private final DatagramSocket mUDPSocket;
    private final SocketAddress mStunServer;
    private MessageHeader mRequest;
    private Set<InetSocketAddress> mAddresses = new HashSet<InetSocketAddress>();

    public STUNBinding(DatagramSocket udpSocket, SocketAddress stunServer) {
        mUDPSocket = udpSocket;
        mStunServer = stunServer;
    }

    private void sendRequest() throws Exception {
        LOGGER.info("performing STUN binding request. Connecting to: {}", mStunServer);

        mRequest = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
        mRequest.generateTransactionID();

        ChangeRequest changeRequest = new ChangeRequest();
        mRequest.addMessageAttribute(changeRequest);

        byte[] data = mRequest.getBytes();
        DatagramPacket p = new DatagramPacket(data, data.length, mStunServer);
        mUDPSocket.send(p);

    }

    private void receiveResponse(DatagramPacket p) {
        try {
            MessageHeader receiveMH = MessageHeader.parseHeader(p.getData());
            receiveMH.parseAttributes(p.getData());
            if (receiveMH.equalTransactionID(mRequest)) {
                final MappedAddress ma = (MappedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);
                final ChangedAddress ca = (ChangedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ChangedAddress);

                LOGGER.info("resolved external address: {}", ma);
                mAddresses.add(new InetSocketAddress(ma.getAddress().getInetAddress(), ma.getPort()));
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    @Override
    public InetSocketAddress call() throws Exception {
        InetSocketAddress retval = null;
        final int packetSize = 64*1024;
        DatagramPacket p = new DatagramPacket(new byte[packetSize], packetSize);
        int numTimeouts = 2;

        mUDPSocket.setSoTimeout(1000);

        sendRequest();

        while(numTimeouts > 0) {
            try {
                mUDPSocket.receive(p);
                receiveResponse(p);
            } catch (SocketTimeoutException e) {
                numTimeouts--;
            }
        }

        retval = Iterables.getFirst(mAddresses, null);
        return retval;
    }
}
