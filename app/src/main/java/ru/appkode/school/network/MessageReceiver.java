package ru.appkode.school.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by lexer on 20.08.14.
 */
public class MessageReceiver {

    private static final String TAG = "MessageReceiver";

    private int mPort = -1;

    public interface OnMessageReceive {
        public void onMessageReceive(String message, InetAddress address);
    }

    private OnMessageReceive mOnMessageReceiveListener;

    public MessageReceiver() {
        Thread thread = new Thread(new ServerThread());
        thread.start();
    }

    public int getPort() {
        return mPort;
    }

    public boolean isInit() {
        if (mPort != -1)
            return true;
        else
            return false;
    }

    public void setOnMessageReceiveListener(OnMessageReceive l) {
        mOnMessageReceiveListener = l;
    }

    private void startConnection(Socket socket) {
        Thread thread = new Thread(new ReceivingThread(socket));
        thread.start();
    }

    class ServerThread implements Runnable {
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(0);
                mPort = serverSocket.getLocalPort();

                Log.d(TAG, "start service on port " + mPort);

                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    startConnection(socket);
                }
            } catch (IOException e) {
                Log.e(TAG, "error with creating server socket " + e.getMessage());
            } finally {
                if (serverSocket != null && !serverSocket.isClosed())
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "error with closing server socket");
                    }
            }
        }

    }

    class ReceivingThread implements Runnable {

        Socket socket;
        ReceivingThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                if (socket != null && !socket.isClosed()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    while (!Thread.currentThread().isInterrupted()) {
                        String message = reader.readLine();
                        if (message == null || message.equals(ConnectionParams.END))
                            break;
                        else if (mOnMessageReceiveListener != null) {
                            mOnMessageReceiveListener.onMessageReceive(message, socket.getInetAddress());
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "error with reading from socket" + e.getMessage());
            } finally {
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "error with closing receive socket " + e.getMessage());
                    }
                }
            }

        }
    }
}
