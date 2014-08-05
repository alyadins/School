package ru.appkode.school.network;

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

    private Socket mSocket = null;
    private InetAddress mHost;
    private int mPort;
    private BufferedReader mReader;
    private PrintWriter mWriter;
    private BlockingQueue<String> mMessageQueue;

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
        mSendingThread = new Thread(new SendingThread());
        mReceivingThread = new Thread(new ReceivingThread());

        mSendingThread.start();
        mReceivingThread.start();
    }
    class SendingThread implements Runnable {

        private int QUEUE_CAPACITY = 10;

        SendingThread() {
            mMessageQueue = new ArrayBlockingQueue<String>(QUEUE_CAPACITY);
        }

        @Override
        public void run() {
            try {
                if (mSocket == null) {
                    mSocket = new Socket(mHost, mPort);
                }
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
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (mSocket != null && !mSocket.isClosed())
                        mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class ReceivingThread implements Runnable {

        @Override
        public void run() {
            try {
                if (mSocket == null) {
                    mSocket = new Socket(mHost, mPort);
                }
                mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                while (!Thread.currentThread().isInterrupted()) {
                    String message = null;
                    message = mReader.readLine();
                    if (message != null) {
                        if (message.equals("END")) {
                            break;
                        }
                        processMessage(message);
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (mSocket != null && !mSocket.isClosed())
                        mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
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
        mMessageQueue.add(message);
    }

    private void processMessage(String message) {
       if (mOnMessageReceivedListener != null) {
           mOnMessageReceivedListener.onReceiveMessage(this, message);
       } else {
           throw new NullPointerException("set OnMessageReceivedListener");
       }
    }

    interface OnMessageReceivedListener {
        public void onReceiveMessage(Connection connection, String message);
    }

    public void setOnMessageReceivedListener(OnMessageReceivedListener l) {
        mOnMessageReceivedListener = l;
    }
}
