package ru.appkode.school.network;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by lexer on 01.08.14.
 */
public class RegistrationServer {

    private static final String TAG = "RegistrationServer";

    private Socket mSocket = null;
    private ServerSocket mServerSocket = null;
    private int mPort = -1;


    public RegistrationServer() {
    }

    class ServerThread implements Runnable {

        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(0);
                mPort = mServerSocket.getLocalPort();

                while (!Thread.currentThread().isInterrupted()) {
                    Log.d(TAG, "Server socket created.");
                    mSocket = mServerSocket.accept();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getPort() {
        return mPort;
    }

    public void start() {
        Thread thread = new Thread(new ServerThread());
        thread.start();
    }
}
