package org.joachimgw;


import lombok.Data;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Paths;
import java.util.Arrays;


/**
 * Streams audio-bytes to the specified endpoint.
 * Transmits packets with data-section size equal to the specified CHUNK_SIZE.
 * Each instance of this class has its own sequence counter, making each stream distinguishable from one another.
 * Has functionality for reading .wav-file as bytes and streaming chunks through UDP-packets.
 */
@Data
public class AudioStreamer {

    private final String serverHostname;
    private final int serverPort;
    private final int chunkSize;
    private long lastPacketSentTime;
    private long bytesSent;
    private long sequence;

    private static final short THROTTLE_DELAY_MS = 20;
    private static final int MAX_BYTES_PER_MS = 200_000;


    /**
     *
     * @param serverHostname The hostname to send packets to.
     * @param serverPort The hostname's port.
     * @param chunkSize Amount of bytes to buffer per file-read.
     */
    AudioStreamer(String serverHostname, int serverPort, int chunkSize) {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
        this.chunkSize = chunkSize;
        this.lastPacketSentTime = System.currentTimeMillis();
        this.bytesSent = 0;
        this.sequence = 1;
    }


    /**
     * Stream byte-chunks of this AudioStreamer's .wav-file.
     */
    public void streamAudio(String wavFilepath) {
        long timestamp;
        String wavFilename = Paths.get(wavFilepath).getFileName().toString();

        try (DatagramSocket streamerSocket = new DatagramSocket();
             BufferedInputStream audioStream = new BufferedInputStream(new FileInputStream(wavFilepath))) {

            InetAddress serverAddress = InetAddress.getByName(serverHostname);
            System.out.printf("AudioStreamer for file '%s' is using port \u001B[1m%d\u001B[0m\n",
                    wavFilename,
                    streamerSocket.getLocalPort());

            // Buffer for reading .wav-bytes
            byte[] readBuffer = new byte[chunkSize];
            int bytesRead;

            // Iterate over bytes of .wav-file
            while ((bytesRead = audioStream.read(readBuffer)) != -1) {

                // Put thread to sleep if the byte rate is higher than the specified max rate
                checkByteRate();

                // Prepare metadata to enter the packet (simulate two RTP-headers)
                timestamp = System.currentTimeMillis();
                String metadata = String.format("%s;%s;", this.sequence, timestamp);
                byte[] metadataBytes = metadata.getBytes();

                byte[] audioDataBytes = (bytesRead == this.chunkSize) ? readBuffer : Arrays.copyOf(readBuffer, bytesRead);

                // Pack metadata and audio with concatenation
                byte[] outgoingData = new byte[metadataBytes.length + audioDataBytes.length];
                System.arraycopy(metadataBytes, 0, outgoingData, 0, metadataBytes.length);
                System.arraycopy(audioDataBytes, 0, outgoingData, metadataBytes.length, audioDataBytes.length);

                // Configure and send outgoing UDP-packet
                DatagramPacket outgoingPacket = new DatagramPacket(outgoingData,
                        outgoingData.length,
                        serverAddress,
                        this.serverPort);
                streamerSocket.send(outgoingPacket);
                this.sequence++;
//                System.out.printf("(%s) SENT PACKET\n -sequence %d\n -timestamp %d\n\n", wavFilename, sequence, timestamp);

                // Update streamer variables
                this.lastPacketSentTime = System.currentTimeMillis();
                this.bytesSent += outgoingData.length;

                // OPTIONAL: Can be used to simulate lower network bandwidth
//                Thread.sleep(2);
            }
        } catch (SocketException e) {
            System.err.printf("Could not create socket. Error: '%s'\n", e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.printf("Could not read file with path '%s'! Error: '%s'\n", wavFilepath, e.getMessage());
        } catch (IOException e) {
            System.err.printf("An IO-related error occurred. Error: '%s'\n", e.getMessage());
        }

    }


    /**
     * Throttle outgoing byte rate if it exceeds the specified threshold.
     */
    private void checkByteRate() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - this.lastPacketSentTime;
        if (this.bytesSent > 0 && elapsedTime > 0) {
            double bytesPerMillisecond = (double) this.bytesSent / elapsedTime;
            if (bytesPerMillisecond > MAX_BYTES_PER_MS) {
                try {
                    Thread.sleep(THROTTLE_DELAY_MS);
                } catch (InterruptedException e) {
                    System.err.printf("Could not correctly manually throttle the thread. Error: '%s'\n", e.getMessage());
                }
            }
        }
    }
}
