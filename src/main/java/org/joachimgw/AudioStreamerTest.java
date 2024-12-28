package org.joachimgw;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Creates and tests the functionality of the AudioStreamer class.
 */
public class AudioStreamerTest {

    public static void main(String[] args) {

        // UDP test server parameters
        final String SERVER_HOSTNAME = "127.0.0.1";
        final int SERVER_PORT = 7000;

        // Amount of bytes to be sent with each packet from the AudioStreamer
        final int CHUNK_SIZE = 1024;

        // Fetch .wav-files from specified directory
        final String WAV_FILEPATH_ROOT = "./src/main/resources/Araujo_TheSagaOfHarrisonCrabfeathers/";
        List<String> wavFiles = AudioStreamerTest.getWAVFilesInDirectory(WAV_FILEPATH_ROOT);

        int nStreamers = wavFiles.size();
        ExecutorService executorService = Executors.newFixedThreadPool(nStreamers);

        // Logging of each thread's elapsed streaming time
        final List<String> elapsedTimes = Collections.synchronizedList(new ArrayList<>());

        // Spawn an AudioStreamer for each .wav-file in the directory 'WAV_FILEPATH_ROOT'
        for (int i = 0; i < nStreamers; i++) {
            int thisIndex = i;

            executorService.submit(() -> {
                long startTime = System.currentTimeMillis();
                AudioStreamer audiostreamer = new AudioStreamer(SERVER_HOSTNAME,
                        SERVER_PORT,
                        CHUNK_SIZE);
                audiostreamer.streamAudio(wavFiles.get(thisIndex));
                long endTime = System.currentTimeMillis();

                // Log elapsed time
                String wavFilename = Paths.get(wavFiles.get(thisIndex)).getFileName().toString();
                elapsedTimes.add(String.format("AudioStreamer for file '%s' finished in \u001B[1m%d\u001B[0m ms (thread %d) ",
                        wavFilename,
                        endTime - startTime,
                        thisIndex));
            });
        }

        // Attempt to join all threads before continuing
        executorService.shutdown();
        try {
            int timeoutSeconds = 30;
            if (!executorService.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                System.out.printf("At least one thread did not complete within the specified timeout of %d seconds.\n",
                        timeoutSeconds);
            }
        } catch (InterruptedException e) {
            System.err.printf("An interruption occurred while awaiting thread joining. Error: '%s'\n",
                    e.getMessage());
        }

        // Display elapsed time for each AudioStreamer
        elapsedTimes.sort(null);
        for (String elapsedTime : elapsedTimes) {
            System.out.printf("\n%s", elapsedTime);
        }
    }


    /**
     * Fetch paths of all .wav files in the specified directory.
     * @param directoryPath The directory to search for .wav-files within.
     * @return List of all .wav file-paths.
     */
    public static List<String> getWAVFilesInDirectory(String directoryPath) {
        List<String> wavFiles = new ArrayList<>();

        try {
            Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".wav"))
                    .forEach(path -> wavFiles.add(path.toString()));
        } catch (IOException e) {
            System.err.printf("An error occurred while reading the directory '%s'. Error: '%s'\n",
                    directoryPath,
                    e.getMessage());
        }

        return wavFiles;
    }
}