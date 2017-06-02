package com.devsmart.supernet.events;


import com.devsmart.supernet.ID;

import java.net.SocketAddress;

public class NewPeerDiscovered {

    public SocketAddress gossipPeer;
    public ID remoteId;
    public SocketAddress socketAddress;

}
