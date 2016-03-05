package org.remote.smartmirror.smartmirror_remote;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RemoteConnection {

    private Handler mUpdateHandler;
    private RemoteControlClient mRemoteControlClient;

    private static final String TAG = "RemoteConnection";

    private Socket mSocket;

    public RemoteConnection(Handler handler) {
        mUpdateHandler = handler;
    }

    public void tearDown() {
        if (mRemoteControlClient != null) {
            mRemoteControlClient.tearDown();
        }
    }

    public void connectToServer(InetAddress address, int port) {
        mRemoteControlClient = new RemoteControlClient(address, port);
    }

    public void sendMessage(String msg) {
        if (mRemoteControlClient != null) {
            mRemoteControlClient.sendMessage(msg);
        }
    }

    public synchronized void updateMessages(String msg, boolean local) {
        Log.e(TAG, "Updating message: " + msg);

        if (local) {
            msg = "local:" + msg;
        }

        Bundle messageBundle = new Bundle();
        messageBundle.putString("msg", msg);

        Message message = new Message();
        message.setData(messageBundle);
        mUpdateHandler.sendMessage(message);

    }

    private synchronized void setSocket(Socket socket) {
        Log.d(TAG, "setSocket being called :: " + socket);
        if (socket == null) {
            Log.d(TAG, "Setting a null socket.");
        }
        if (mSocket != null) {
            if (mSocket.isConnected()) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        mSocket = socket;
    }

    private Socket getSocket() {
        return mSocket;
    }

    private class RemoteControlClient {

        private InetAddress mAddress;
        private int PORT;

        private final String TAG = "RemoteControlClient";

        private Thread mSendThread;
        private Thread mRecThread;

        public RemoteControlClient(InetAddress address, int port) {

            Log.d(TAG, "Creating remoteClient");
            this.mAddress = address;
            this.PORT = port;

            mSendThread = new Thread(new SendingThread());
            mSendThread.start();
        }

        class SendingThread implements Runnable {

            BlockingQueue<String> mMessageQueue;
            private int QUEUE_CAPACITY = 10;

            public SendingThread() {
                mMessageQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
            }

            @Override
            public void run() {
                try {
                    //if (getSocket() == null) {
                        setSocket(new Socket(mAddress, PORT));
                        Log.d(TAG, "Client-side socket initialized.");

                    //} else {
                    //    Log.d(TAG, "Socket already initialized. skipping!");
                    //}

                    mRecThread = new Thread(new ReceivingThread());
                    mRecThread.start();

                } catch (UnknownHostException e) {
                    Log.d(TAG, "Initializing socket failed, UHE", e);
                } catch (IOException e) {
                    Log.d(TAG, "Initializing socket failed, IOE.", e);
                }

                while (true) {
                    try {
                        String msg = mMessageQueue.take();
                        sendMessage(msg);
                    } catch (InterruptedException ie) {
                        Log.d(TAG, "Message sending loop interrupted, exiting");
                    }
                }
            }
        }

        class ReceivingThread implements Runnable {

            @Override
            public void run() {

                BufferedReader input;
                try {
                    input = new BufferedReader(new InputStreamReader(
                            mSocket.getInputStream()));
                    while (!Thread.currentThread().isInterrupted()) {

                        String messageStr = null;
                        messageStr = input.readLine();
                        if (messageStr != null) {
                            Log.d(TAG, "Read from the stream: " + messageStr);
                            updateMessages(messageStr, false);
                        } else {
                            Log.d(TAG, "The nulls! The nulls!");
                            break;
                        }
                    }
                    input.close();
                } catch (IOException e) {
                    Log.i(TAG, "receiving thread closed");
                }
            }
        }

        public void tearDown() {
            try {
                Log.i(TAG, "closing socket");
                if (getSocket() != null)
                    getSocket().close();
            } catch (IOException ioe) {
                Log.e(TAG, "Error when closing server socket.");
            }
        }

        public void sendMessage(String msg) {
            try {
                Socket socket = getSocket();
                if (socket == null) {
                    Log.d(TAG, "Socket is null, wtf?");
                } else if (socket.getOutputStream() == null) {
                    Log.d(TAG, "Socket output stream is null, wtf?");
                }

                PrintWriter out = new PrintWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(getSocket().getOutputStream())), true);
                out.println(msg);
                out.flush();
                updateMessages(msg, true);
            } catch (UnknownHostException e) {
                Log.d(TAG, "Unknown Host", e);
            } catch (IOException e) {
                Log.d(TAG, "I/O Exception", e);
            } catch (Exception e) {
                Log.d(TAG, "Error3", e);
            }
            Log.d(TAG, "Client sent message: " + msg);
        }
    }
}

