package com.devsmart.supernet;

import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final Pattern REGEX_ADDRESS = Pattern.compile("([0-9a-zA-Z\\.]*):([0-9]+)");

    public static InetSocketAddress parseSocketAddress(String str) {
        Matcher m = REGEX_ADDRESS.matcher(str);
        if(m.find()) {
            String name = m.group(1);
            int port = Integer.parseInt(m.group(2));
            return new InetSocketAddress(name, port);
        }
        return null;
    }
}
