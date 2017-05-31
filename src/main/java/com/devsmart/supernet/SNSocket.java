package com.devsmart.supernet;


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SNSocket implements Closeable {

    public ID getRemoteAddress() {
        return null;
    }

    public int getRemotePort() {
        return -1;
    }


    public OutputStream getOutputStream(){
        return null;
    }

    public InputStream getInputStream() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
