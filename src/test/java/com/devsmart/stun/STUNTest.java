package com.devsmart.stun;


import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.header.MessageHeader;
import org.junit.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class STUNTest {


    @Test
    public void testStunRequest() throws Exception {
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

        DatagramPacket p = new DatagramPacket(data, data.length, InetAddress.getByName("stun.l.google.com"), 19302);
        s.send(p);

        DatagramPacket rp;

        rp = new DatagramPacket(new byte[32], 32);

        s.receive(rp);
        MessageHeader receiveMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingResponse);
        // System.out.println(receiveMH.getTransactionID().toString() + "Size:"
        // + receiveMH.getTransactionID().length);
        receiveMH.parseAttributes(rp.getData());
        MappedAddress ma = (MappedAddress) receiveMH
                .getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);
        System.out.println(ma.getAddress()+" "+ma.getPort());
    }
}
