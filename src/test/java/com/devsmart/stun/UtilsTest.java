package com.devsmart.stun;

import com.devsmart.supernet.Utils;
import org.junit.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;

public class UtilsTest {


    @Test
    public void testWritingIPv4SocketAddress() throws Exception {

        byte[] byteAddress = new byte[] {
                (byte) 0x00,
                (byte) 0x01,
                (byte) 0x02,
                (byte) 0x03
        };

        InetAddress address = Inet4Address.getByAddress(byteAddress);
        final int port = 8888;

        byte[] buf = new byte[6];
        Utils.writeIPv4SocketAddress((Inet4Address) address, port, buf, 0);


        InetSocketAddress readValue = Utils.readIPv4SocketAddress(buf, 0);

        assertEquals(address, readValue.getAddress());
        assertEquals(port, readValue.getPort());

    }
}
