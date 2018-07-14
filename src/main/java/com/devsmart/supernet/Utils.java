package com.devsmart.supernet;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private static final Pattern REGEX_ADDRESS = Pattern.compile("([0-9a-zA-Z\\.\\-]*):([0-9]+)");

    public static InetSocketAddress parseSocketAddress(String str) {
        Matcher m = REGEX_ADDRESS.matcher(str);
        if(m.find()) {
            String name = m.group(1);
            int port = Integer.parseInt(m.group(2));
            return new InetSocketAddress(name, port);
        }
        return null;
    }

    public static void writeIPv4SocketAddress(Inet4Address socketAddress, int port, byte[] buf, int offset) {

        byte[] address = socketAddress.getAddress();

        System.arraycopy(address, 0, buf, offset, 4);


        buf[offset + 4] = (byte) ((port >>> 8) & 0xFF);
        buf[offset + 5] = (byte) (port & 0xFF);
    }

    public static InetSocketAddress readIPv4SocketAddress(byte[] buf, int offset) {
        try {
            byte[] addressBytes = new byte[4];
            System.arraycopy(buf, offset, addressBytes, 0, 4);
            InetAddress address = Inet4Address.getByAddress(addressBytes);
            int port = (0xFF & buf[offset + 4]) << 8;
            port |= (0xFF & buf[offset + 5]);

            return new InetSocketAddress(address, port);
        } catch (UnknownHostException e) {
            LOGGER.error("", e);
            Throwables.propagate(e);
            return null;
        }
    }


}
