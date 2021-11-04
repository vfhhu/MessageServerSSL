package com.threex.lib.connect;

/**
 * Created by leo on 2017/6/22.
 */

public interface OnSocketClientListener {
    void onSend(String src,SocketClient client);
    void onData(String src,SocketClient client);
    void onConnected(SocketClient client);
    void onClosed(SocketClient client);
    void onError(String err,SocketClient client);

}
