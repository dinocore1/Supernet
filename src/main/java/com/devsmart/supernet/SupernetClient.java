package com.devsmart.supernet;


import com.google.common.base.Preconditions;
import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.ChangedAddress;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.header.MessageHeaderParsingException;
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
    private boolean mUDPSocketRunning;
    private STUNBindingRequest mSTUNBindingRequest;

    SupernetClient() {}

    public static class Builder {

        private ID mId;
        private int mUDPPort = 11382;
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
            //Preconditions.checkState(mId != null);

            SupernetClient retval = new SupernetClient();
            retval.mClientId = mId;
            retval.mSTUNServer = mSTUNServer;
            retval.mUDPSocket = new DatagramSocket();
            retval.mUDPSocket.setReuseAddress(true);
            //retval.mUDPSocket.bind(new InetSocketAddress(mUDPPort));
            return retval;
        }
    }

    public void start() {
        try {
            mAddresses.clear();
            mAddresses.add(mUDPSocket.getLocalSocketAddress());


            mUDPReceiveThread = new Thread(mReceiveUDPTask, "Receive UDP");
            mUDPReceiveThread.start();

            doSTUNBindingRequest();

        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    public void shutdown() {
        try {
            if (mUDPReceiveThread != null) {
                mUDPSocketRunning = false;
                mUDPReceiveThread.join();
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }

    }

    private void doSTUNBindingRequest() throws Exception {
        mSTUNBindingRequest = new STUNBindingRequest();
        mSTUNBindingRequest.doIt();
    }

    private class STUNBindingRequest implements PacketReceiver {

        private MessageHeader mSendMH;

        void doIt() throws Exception {
            LOGGER.info("performing STUN binding request. Connecting to: ");

            mSendMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
            mSendMH.generateTransactionID();

            ChangeRequest changeRequest = new ChangeRequest();
            mSendMH.addMessageAttribute(changeRequest);

            byte[] data = mSendMH.getBytes();
            DatagramPacket p = new DatagramPacket(data, data.length, mSTUNServer);
            mUDPSocket.send(p);
        }

        @Override
        public boolean receive(DatagramPacket packet) {
            try {
                MessageHeader receiveMH = MessageHeader.parseHeader(packet.getData());
                receiveMH.parseAttributes(packet.getData());
                if(receiveMH.equalTransactionID(mSendMH)) {
                    MappedAddress ma = (MappedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);
                    ChangedAddress ca = (ChangedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ChangedAddress);

                    LOGGER.info("resolved external address: {}", ma);
                    mSTUNBindingRequest = null;

                    return true;
                }

            } catch (Exception e) {
                return false;
            }

            return false;
        }
    }

    public interface PacketReceiver {

        /**
         *
         * @param packet
         * @return true if the the packet was handled by this function
         */
        boolean receive(DatagramPacket packet);
    }



    private final Runnable mReceiveUDPTask = new Runnable() {

        private DatagramPacket createPacket() {
            final int packetSize = 64*1024;
            return new DatagramPacket(new byte[packetSize], packetSize);
        }

        @Override
        public void run() {
            mUDPSocketRunning = true;
            while(mUDPSocketRunning) {
                DatagramPacket receivedPacket = createPacket();
                try {
                    mUDPSocket.receive(receivedPacket);

                    if(mSTUNBindingRequest != null && mSTUNBindingRequest.receive(receivedPacket)) {

                        //handled successfully
                    }


                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
            LOGGER.info("exiting UDP receive thread: {}", mUDPSocket);

        }
    };



}
