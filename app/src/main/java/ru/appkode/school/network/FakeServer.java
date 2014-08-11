package ru.appkode.school.network;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by lexer on 11.08.14.
 */
public class FakeServer {
    private int mPort = -1;

    public FakeServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket socket = new ServerSocket(0);
                    mPort = socket.getLocalPort();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            }).start();
    }

    public int getPort() {
        return mPort;
    }
}
