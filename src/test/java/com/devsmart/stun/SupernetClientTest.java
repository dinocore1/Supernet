package com.devsmart.stun;

import com.devsmart.supernet.SupernetClient;
import org.junit.Test;

import java.io.IOException;


public class SupernetClientTest {


    @Test
    public void testStartClient() throws Exception {

        SupernetClient client = new SupernetClient.Builder().build();
        client.start();

        Thread.sleep(10 * 1000);

        client.shutdown();
    }
}
