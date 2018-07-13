package com.devsmart.stun;


import com.devsmart.supernet.STUNBinding;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.Assert.assertNotNull;

public class STUNTest {
    private DatagramSocket mSocket;

    @Before
    public void setupSocket() throws Exception {
        mSocket = new DatagramSocket();
    }

    @After
    public void shutdownSocket() throws Exception {
        mSocket.close();
    }

    @Test
    public void testSTUNBinding() throws Exception {
        InetSocketAddress googleStun = new InetSocketAddress(InetAddress.getByName("stun.l.google.com"), 19302);

        STUNBinding binding = new STUNBinding(mSocket, googleStun);
        InetSocketAddress result = binding.call();
        assertNotNull(result);

    }
}
