package ru.appkode.school.network;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by lexer on 20.08.14.
 */
public class MessageSender {

    private static final String TAG = "MessageSender";

    public void sendMessage(String message, InetAddress address, String port) {
        Thread thread = new Thread(new SendingThread(message, address, Integer.parseInt(port)));
        thread.start();
    }

    public void sendMessage(String message, InetAddress address, int port) {
        Thread thread = new Thread(new SendingThread(message, address, port));
        thread.start();
    }

    class SendingThread implements Runnable {
        String message;
        InetAddress address;
        int port;

        SendingThread(String message, InetAddress address, int port) {
            this.message = message;
            this.address = address;
            this.port = port;
        }

        @Override
        public void run() {
            Socket socket = null;
            try {
                socket = new Socket(address, port);

                Log.d(TAG, "send message to " + address + " " + port);

                PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                writer.println(message);
                writer.println(ConnectionParams.END);
                writer.flush();
            } catch (UnknownHostException e) {
                Log.e(TAG, "unknown host exception " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "error when create socket" + e.getMessage());
            } finally {
                if (socket != null && !socket.isClosed())
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "socket closed error " + e.getMessage());
                    }
            }
        }
    }
}
