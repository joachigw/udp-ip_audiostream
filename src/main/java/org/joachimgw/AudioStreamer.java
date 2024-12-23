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
import java.util.Arrays;


/**
 * Reads bytes in .wav-file and streams via UDP in chunks to the specified server/endpoint.
 */
@Data
public class AudioStreamer {

    private final String serverHostname;
    private final int serverPort;
    private final String wavFilepath;
    private long sequence = 1;
    private static final int CHUNK_SIZE = 1024;


    /**
     * Constructor.
     * @param serverHostname The hostname to send packets to.
     * @param serverPort The hostname's port.
     * @param wavFilepath The path of the .wav-file to stream.
     */
    AudioStreamer(String serverHostname, int serverPort, String wavFilepath) {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
        this.wavFilepath = wavFilepath;
    }


    /**
     * Stream byte-chunks of this AudioStreamer's .wav-file.
     */
    public void streamAudio() {
        long timestamp;
        String wavFilename = this.wavFilepath.split("/")[this.wavFilepath.split("/").length - 1];

        try (DatagramSocket streamerSocket = new DatagramSocket();
             BufferedInputStream audioStream = new BufferedInputStream(new FileInputStream(this.wavFilepath))) {

            streamerSocket.setSoTimeout(3000);

            InetAddress serverAddress = InetAddress.getByName(serverHostname);
            System.out.printf("(%s) This AudioStreamer is using port \u001B[1m%d\u001B[0m\n", wavFilename, streamerSocket.getLocalPort());

            // Buffer for reading .wav-bytes
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead = 0;

            // Iterate over bytes of .wav-file
            while ((bytesRead = audioStream.read(buffer)) != -1) {

                // Prepare metadata to enter the packet (simulate RTP-headers)
                timestamp = System.currentTimeMillis();
                String metadata = String.format("%s;%s;", sequence, timestamp);
                byte[] metadataBytes = metadata.getBytes();

                byte[] audioDataBytes = (bytesRead == CHUNK_SIZE) ? buffer : Arrays.copyOf(buffer, bytesRead);

                // Pack metadata and audio bytes into bytes for outgoing packet
                byte[] outgoingData = new byte[metadataBytes.length + audioDataBytes.length];
                System.arraycopy(metadataBytes, 0, outgoingData, 0, metadataBytes.length);
                System.arraycopy(audioDataBytes, 0, outgoingData, metadataBytes.length, audioDataBytes.length);

                // Configure and send outgoing UDP-packet
                DatagramPacket outgoingPacket = new DatagramPacket(outgoingData,
                        outgoingData.length,
                        serverAddress,
                        this.serverPort);
                streamerSocket.send(outgoingPacket);
//                System.out.printf("(%s) SENT PACKET\n -sequence %d\n -timestamp %d\n\n", wavFilename, sequence, timestamp);
                sequence++;

                // OPTIONAL: Can be used to simulate lower network bandwidth
//                Thread.sleep(2);
            }
        } catch (SocketException e) {
            System.out.printf("Could not create socket. Error: '%s'\n", e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.printf("Could not read file with path '%s'!\n", this.wavFilepath);
        } catch (IOException e) {
            System.out.printf("An IO-related error occurred. Error: '%s'\n", e.getMessage());
        }
    }
}
