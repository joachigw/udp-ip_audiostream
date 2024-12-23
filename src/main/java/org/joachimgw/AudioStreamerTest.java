package org.joachimgw;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Creates and tests the functionality of the AudioStreamer class.
 */
public class AudioStreamerTest {

    public static void main(String[] args) {

        // UDP test server parameters
        final String SERVER_HOSTNAME = "127.0.0.1";
        final int SERVER_PORT = 7000;

        // Fetch .wav-files from specified directory
        final String WAV_FILEPATH_ROOT = "./src/main/resources/Araujo_TheSagaOfHarrisonCrabfeathers";
        List<String> wavFiles = AudioStreamerTest.getWAVFilesInDirectory(WAV_FILEPATH_ROOT);

        int nStreamers = wavFiles.size();
        ExecutorService executorService = Executors.newFixedThreadPool(nStreamers);

        final List<String> elapsedTimes = Collections.synchronizedList(new ArrayList<>());

        // Spawn an AudioStreamer for each .wav-file in the directory 'WAV_FILEPATH_ROOT'
        for (int i = 0; i < nStreamers; i++) {
            int thisIndex = i;

            executorService.submit(() -> {
                long startTime = System.currentTimeMillis();
                AudioStreamer audiostreamer = new AudioStreamer(SERVER_HOSTNAME, SERVER_PORT, wavFiles.get(thisIndex));
                audiostreamer.streamAudio();
                long endTime = System.currentTimeMillis();

                elapsedTimes.add(String.format("(thread %d) AudioStreamer for file '%s' finished in \u001B[1m%d\u001B[0m ms",
                        thisIndex,
                        wavFiles.get(thisIndex).split("/")[wavFiles.get(thisIndex).split("/").length-1],
                        endTime - startTime));
            });
        }

        // Attempt to join all threads before continuing
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS)) {
                System.out.println("Some tasks did not finish within the timeout.");
            }
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted while waiting for tasks to finish.");
        }

        // Display elapsed time for each AudioStreamer
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
            System.err.printf("Could not read directory. Error: '%s'\n", e.getMessage());
        }

        return wavFiles;
    }
}