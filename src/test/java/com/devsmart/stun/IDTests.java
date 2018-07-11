package com.devsmart.stun;

import com.devsmart.supernet.ID;
import org.junit.Test;

import static org.junit.Assert.*;

public class IDTests {

    @Test
    public void testNumSharedBits() {

        byte[] data = new byte[ID.NUM_BYTES];

        ID a = new ID(data, 0);
        ID b = new ID(data, 0);

        assertEquals(ID.NUM_BYTES*8, a.getNumSharedPrefixBits(b));

        data[0] = 0x70;
        b = new ID(data, 0);
        assertEquals(1, a.getNumSharedPrefixBits(b));

        data[0] = 0x30;
        b = new ID(data, 0);
        assertEquals(2, a.getNumSharedPrefixBits(b));

        data[0] = 0x10;
        b = new ID(data, 0);
        assertEquals(3, a.getNumSharedPrefixBits(b));

    }

    @Test
    public void testCompare() {

        byte[] data = new byte[ID.NUM_BYTES];

        ID a = new ID(data, 0);
        ID b = new ID(data, 0);

        assertEquals(0, a.compareTo(b));

        data[0] = 0x70;
        b = new ID(data, 0);
        assertTrue(a.compareTo(b) < 0);

        data[0] = (byte) 0xFF;
        a = new ID(data, 0);
        assertTrue(a.compareTo(b) > 0);


    }
}
