package com.devsmart.supernet;


import com.google.common.base.Preconditions;
import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.header.MessageHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class SupernetClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupernetClient.class);

    private ID mClientId;
    private DatagramSocket mUDPSocket;
    private final ArrayList<SocketAddress> mAddresses = new ArrayList<SocketAddress>();
    private SocketAddress mSTUNServer;
    private Thread mUDPReceiveThread;

    SupernetClient() {}

    public static class Builder {

        private ID mId;
        private int mUDPPort = 20382;
        private SocketAddress mSTUNServer = new InetSocketAddress("stun.l.google.com", 19302);

        public Builder withId(ID id) {
            mId = id;
            return this;
        }

        public Builder withUDPPort(int port) {
            mUDPPort = port;
            return this;
        }

        public SupernetClient build() throws IOException {
            Preconditions.checkState(mId != null);

            SupernetClient retval = new SupernetClient();
            retval.mClientId = mId;
            retval.mSTUNServer = mSTUNServer;
            retval.mUDPSocket = new DatagramSocket();
            retval.mUDPSocket.setReuseAddress(true);
            retval.mUDPSocket.bind(new InetSocketAddress(mUDPPort));
            return retval;
        }
    }

    public void start() {
        try {
            mAddresses.clear();
            mAddresses.add(mUDPSocket.getLocalSocketAddress());
            doSTUNBindingRequest();

            mUDPReceiveThread = new Thread(mReceiveUDPTask, "Receive UDP");
            mUDPReceiveThread.start();

        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    private void doSTUNBindingRequest() throws Exception {

        LOGGER.info("performing STUN binding request. Connecting to: ")
        MessageHeader sendMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
        // sendMH.generateTransactionID();

        // add an empty ChangeRequest attribute. Not required by the
        // standard,
        // but JSTUN server requires it

        ChangeRequest changeRequest = new ChangeRequest();
        sendMH.addMessageAttribute(changeRequest);

        byte[] data = sendMH.getBytes();


        DatagramSocket s = new DatagramSocket();
        s.setReuseAddress(true);

        DatagramPacket p = new DatagramPacket(data, data.length, mSTUNServer);
        s.send(p);

        DatagramPacket rp;

        rp = new DatagramPacket(new byte[32], 32);

        s.receive(rp);
        MessageHeader receiveMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingResponse);
        receiveMH.parseAttributes(rp.getData());
        MappedAddress ma = (MappedAddress) receiveMH
                .getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);

        InetSocketAddress externalAddress = new InetSocketAddress(ma.getAddress().getInetAddress(), ma.getPort());
        LOGGER.info("resolved external address: {}", externalAddress);
        mAddresses.add(externalAddress);
    }

    private final Runnable mReceiveUDPTask = new Runnable() {

        @Override
        public void run() {

        }
    };



}
