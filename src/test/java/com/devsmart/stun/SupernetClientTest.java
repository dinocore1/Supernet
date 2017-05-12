package com.devsmart.stun;

import com.devsmart.supernet.ID;
import com.devsmart.supernet.SupernetClient;
import org.junit.Test;

import java.util.Random;


public class SupernetClientTest {


    @Test
    public void testStartClient() throws Exception {

        SupernetClient client = new SupernetClient.Builder()
                .withId(ID.createRandom(new Random()))
                .build();

        client.start();

        Thread.sleep(2 * 1000);

        client.shutdown();
    }
}
