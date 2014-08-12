package ru.appkode.school.network;

import android.util.Log;

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
                    socket.close();
                    Log.d("TEST", "fake server started at port " + mPort);
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
