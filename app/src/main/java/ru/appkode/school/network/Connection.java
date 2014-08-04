package ru.appkode.school.network;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by lexer on 04.08.14.
 */
public class Connection {

    private Socket mSocket;
    private BufferedReader mReader;
    private PrintWriter mWriter;
    private BlockingQueue<String> mMessageQueue;

    private Thread mSendingThread;
    private Thread mReceivingThread;

    public Connection(Socket socket) {
        mSocket = socket;
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
                mWriter.close();
            }
        }
    }

    class ReceivingThread implements Runnable {

        @Override
        public void run() {
            try {
                try {
                    mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                    while (!Thread.currentThread().isInterrupted()) {
                        String message = null;
                        message = mReader.readLine();
                        if (message != null) {
                            processMessage(message);
                        }
                    }
                } finally {
                    mReader.close();
                }
            } catch (IOException io) {
                io.printStackTrace();
            }
        }


    }

    public void sendMessage(String message) {
        mMessageQueue.add(message);
    }

    private void processMessage(String message) {
        Log.d("TEST", "message recived = " + message);
    }
}
