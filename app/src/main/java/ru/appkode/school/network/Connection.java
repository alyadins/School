package ru.appkode.school.network;

import android.util.Log;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by lexer on 04.08.14.
 */
public class Connection {

    interface OnMessageReceivedListener {
        public void onReceiveMessage(Connection connection, String message) throws JSONException;
    }

    public void setOnMessageReceivedListener(OnMessageReceivedListener l) {
        mOnMessageReceivedListener = l;
    }

    private Socket mSocket = null;
    private InetAddress mHost;
    private int mPort;
    private BufferedReader mReader;
    private PrintWriter mWriter;
    private BlockingQueue<String> mMessageQueue;

    private Thread mConnectionThread;
    private Thread mSendingThread;
    private Thread mReceivingThread;

    private OnMessageReceivedListener mOnMessageReceivedListener;

    public Connection(InetAddress host, int port) {
        mHost = host;
        mPort = port;
    }

    public Connection(Socket socket) {
        mSocket = socket;
        mPort = socket.getLocalPort();
    }

    public void start() throws IOException {
        mConnectionThread = new Thread(new ConnectionThread());
        mConnectionThread.start();
    }
    class ConnectionThread implements Runnable {

        private int QUEUE_CAPACITY = 10;
        ConnectionThread() {
            mMessageQueue = new ArrayBlockingQueue<String>(QUEUE_CAPACITY);
        }

        @Override
        public void run() {
            if (mSocket == null) {
                createSocket(mHost, mPort);
            }
            mSendingThread = new Thread(new SendingThread());
            mReceivingThread = new Thread(new ReceivingThread());

            mSendingThread.start();
            mReceivingThread.start();
        }
    }
    class SendingThread implements Runnable {

        @Override
        public void run() {
            try {
                if (mSocket != null && !mSocket.isClosed()) {
                    mWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())), true);
                    while (true) {
                        try {
                            String message = mMessageQueue.take();
                            mWriter.println(message);
                            mWriter.flush();
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeConnection();
            }
        }
    }

    class ReceivingThread implements Runnable {

        @Override
        public void run() {
            try {
                if (mSocket != null && !mSocket.isClosed()) {
                    mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                    while (!Thread.currentThread().isInterrupted()) {
                        String message = null;
                        if (mSocket.isClosed()) {
                            break;
                        }
                        message = mReader.readLine();
                        if (message != null && !mSocket.isClosed()) {
                            if (message.equals("END")) {
                                break;
                            }
                            processMessage(message);
                        } else {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
            } finally {
               closeConnection();
            }

        }
    }

    synchronized boolean createSocket(InetAddress host, int port) {
        try {
            mSocket = new Socket(host, port);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean isConnected() {
        if (mSocket != null && !mSocket.isClosed())
            return true;
        return false;
    }

    public int getPort() {
        return mPort;
    }

    public void sendMessage(String message) {
        Log.d("CONNECTION", "send message" + message);
        mMessageQueue.add(message);
    }

    private void processMessage(String message) {
        Log.d("CONNECTION", "recieve message" + message);
       if (mOnMessageReceivedListener != null) {
           try {
               mOnMessageReceivedListener.onReceiveMessage(this, message);
           } catch (JSONException e) {
               e.printStackTrace();
           }
       } else {
           throw new NullPointerException("set OnMessageReceivedListener");
       }
    }

    public void closeConnection() {
        if (mSocket != null && !mSocket.isClosed()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
