package com.devsmart.supernet;

import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.ChangedAddress;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.header.MessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class STUNBindingRequest implements PacketReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(STUNBindingRequest.class);

    private final SupernetClientImp mClient;
    private final SocketAddress mServerAddress;
    private MessageHeader mSendMH;

    public STUNBindingRequest(SupernetClientImp client, SocketAddress stunServerAddress) {
        mClient = client;
        mServerAddress = stunServerAddress;
    }

    public void doIt() throws Exception {
        LOGGER.info("performing STUN binding request. Connecting to: {}", mServerAddress);

        mSendMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
        mSendMH.generateTransactionID();

        ChangeRequest changeRequest = new ChangeRequest();
        mSendMH.addMessageAttribute(changeRequest);

        byte[] data = mSendMH.getBytes();
        DatagramPacket p = new DatagramPacket(data, data.length, mServerAddress);
        mClient.mUDPSocket.send(p);
    }

    @Override
    public boolean receive(DatagramPacket packet) {
        try {
            MessageHeader receiveMH = MessageHeader.parseHeader(packet.getData());
            receiveMH.parseAttributes(packet.getData());
            if (receiveMH.equalTransactionID(mSendMH)) {
                final MappedAddress ma = (MappedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);
                final ChangedAddress ca = (ChangedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ChangedAddress);

                LOGGER.info("resolved external address: {}", ma);

                mClient.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mClient.mAddresses.add(new InetSocketAddress(ma.getAddress().getInetAddress(), ma.getPort()));
                            mClient.unregisterReceiver(STUNBindingRequest.this);
                        } catch (Exception e) {
                            LOGGER.error("", e);
                        }

                    }
                });

                return true;
            }

        } catch (Exception e) {
            return false;
        }

        return false;
    }
}