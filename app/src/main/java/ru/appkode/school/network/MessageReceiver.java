package ru.appkode.school.network;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by lexer on 20.08.14.
 */
public class MessageReceiver {

    private static final String TAG = "MessageReceiver";

    private String mIp;
    private int mPort;

    public interface OnMessageReceive {
        public void onMessageRecieve(String message);
    }

    private OnMessageReceive mOnMessageReceiveListener;

    public MessageReceiver() {
        Thread thread = new Thread(new ServerThread());
        thread.start();
    }

    public String getIp() {
        return mIp;
    }

    public int getPort() {
        return mPort;
    }

    public void setOnMessageReceiveListener(OnMessageReceive l) {
        mOnMessageReceiveListener = l;
    }

    private void startConnection(Socket socket) {

    }

    class ServerThread implements Runnable {
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(0);
                mPort = serverSocket.getLocalPort();
                mIp = serverSocket.getInetAddress().getHostAddress();
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
                        if (message == null || message.equals("END"))
                            break;
                        else if (mOnMessageReceiveListener != null) {
                            mOnMessageReceiveListener.onMessageRecieve(message);
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
