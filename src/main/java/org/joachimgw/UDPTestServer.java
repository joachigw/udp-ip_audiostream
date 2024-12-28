package org.joachimgw;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


/**
 * UDP server for testing purposes. Only presents the received/incoming packets' contents.
 */
public class UDPTestServer {

    public static void main(String[] args) {
        final int PORT_NUMBER = 7000;
        byte[] incomingData = new byte[2048];

        try (DatagramSocket serverSocket = new DatagramSocket(PORT_NUMBER)) {

            System.out.printf("Server listening on port %d...\n", PORT_NUMBER);

            // Waits for incoming packets and handles them when they arrive
            DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
            while (true) {
                serverSocket.receive(incomingPacket);
                long currTimestamp = System.currentTimeMillis();

                // Extract sender data
                String senderHostname = incomingPacket.getAddress().getHostName();
                int senderPort = incomingPacket.getPort();

                // Extract packet data
                String receivedData = new String(incomingPacket.getData(), 0, incomingPacket.getLength());

                // Verify separators between metadata and audio data
                int sequenceTimestampSeparator = receivedData.indexOf(";");
                int timestampDataSeparator = receivedData.indexOf(";", sequenceTimestampSeparator + 1);
                if (sequenceTimestampSeparator == -1) {
                    throw new IllegalArgumentException("Invalid packet format received. Could not parse 'sequence'-metadata.");
                }
                if (timestampDataSeparator == -1) {
                    throw new IllegalArgumentException("Invalid packet format received. Could not parse 'timestamp'-metadata.");
                }

                // Extract sequence number and UNIX-timestamp
                long sequence = Long.parseLong(receivedData.substring(0, sequenceTimestampSeparator));
                long timestamp = Long.parseLong(receivedData.substring(sequenceTimestampSeparator + 1, timestampDataSeparator));

                // Extract audio data
                byte[] audioData = receivedData.substring(timestampDataSeparator + 1).getBytes();

                System.out.printf("""
                                RECEIVED DATAGRAM
                                 -hostname   %s
                                 -port       %d
                                 -sequence   %d
                                 -timestamp  %d
                                 -audio data %d (bytes)
                                 -latency    %d (ms)
                                
                                """,
                        senderHostname, senderPort, sequence, timestamp, audioData.length, currTimestamp-timestamp);
            }
        } catch (IOException e) {
            System.err.printf("An IO-related error occurred. Error: '%s'\n", e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.printf("Error parsing metadata from received packet. Error: '%s'\n", e.getMessage());
        }
    }
}
