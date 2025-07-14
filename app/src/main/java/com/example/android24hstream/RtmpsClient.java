package com.example.android24hstream;

import android.media.MediaCodec;
import android.util.Log;

import java.io.IOException;
import javax.net.ssl.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;

public class RtmpsClient {

    private final String host;
    private final int port;
    private static final String TAG = "RtmpsClient";

    private SSLSocket sslSocket;

    private MediaCodec mEncoder;

    public RtmpsClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() {
        // Run connection in background thread
        new Thread(() -> {
            try {
                // Step 1: Create SSL context and factory
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, null, null);
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                // Step 2: Create SSL socket
                sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);

                // Step 3: Add handshake listener
                sslSocket.addHandshakeCompletedListener(event -> {
                    Log.i(TAG, "Handshake completed successfully!");
                    Log.i(TAG, "Cipher Suite: " + event.getCipherSuite());
                    Log.i(TAG, "Session: " + event.getSession());
                    try {
                        Principal peer = event.getPeerPrincipal();
                        Log.i(TAG, "Peer Principal: " + peer.getName());
                    } catch (SSLPeerUnverifiedException e) {
                        Log.e(TAG, "Peer not verified", e);
                    }
                });

                // Step 4: Perform handshake
                sslSocket.startHandshake();

                Log.i(TAG, "Connected to RTMPS server at " + host + ":" + port);

            } catch (GeneralSecurityException | IOException e) {
                Log.e(TAG, "Failed to connect: " + e.getMessage(), e);
            }
        }).start();
    }

    private void performRtmpHandshake() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Ensure socket is already connected (sslSocket must be initialized)
                OutputStream outputStream = sslSocket.getOutputStream();
                InputStream inputStream = sslSocket.getInputStream();

                int timestamp = (int) System.currentTimeMillis();
                byte[] randomData = new byte[1528];
                new Random().nextBytes(randomData);

                // Build C0 + C1
                byte[] handshake = new byte[1537];
                handshake[0] = 3; // C0 - RTMP version

                // C1
                byte[] timestampBytes = ByteBuffer.allocate(4).putInt(timestamp).array();
                handshake[1] = timestampBytes[0];
                handshake[2] = timestampBytes[1];
                handshake[3] = timestampBytes[2];
                handshake[4] = timestampBytes[3];

                // 4 zero bytes
                handshake[5] = 0;
                handshake[6] = 0;
                handshake[7] = 0;
                handshake[8] = 0;

                // Copy random data into handshake
                System.arraycopy(randomData, 0, handshake, 9, randomData.length);

                // Send C0 + C1
                outputStream.write(handshake);
                outputStream.flush();

                // Read S0 + S1
                byte[] response = new byte[1537];
                int bytesRead = inputStream.read(response);
                if (bytesRead != 1537 || response[0] != 3) {
                    throw new IllegalStateException("Invalid RTMP handshake version from server");
                }

                byte[] s1 = Arrays.copyOfRange(response, 1, 1537);

                // Build C2
                byte[] c2 = new byte[1536];

                // Copy first 4 bytes of S1
                System.arraycopy(s1, 0, c2, 0, 4);

                // Current timestamp
                byte[] currentTimestamp = ByteBuffer.allocate(4).putInt((int) System.currentTimeMillis()).array();
                System.arraycopy(currentTimestamp, 0, c2, 4, 4);

                // Copy the rest of S1 (from index 8)
                System.arraycopy(s1, 8, c2, 8, 1528);

                // Send C2
                outputStream.write(c2);
                outputStream.flush();

                // Read S2
                byte[] s2 = new byte[1536];
                inputStream.read(s2);

                Log.i(TAG, "RTMP handshake successful");
            } catch (Exception e) {
                Log.e(TAG, "Handshake failed: " + e.getMessage(), e);
            }
        });
    }




    private MediaCodec getEncoder() {
        if (mEncoder == null) {
            try {
                mEncoder = MediaCodec.createEncoderByType("video/avc");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mEncoder;
    }

}


