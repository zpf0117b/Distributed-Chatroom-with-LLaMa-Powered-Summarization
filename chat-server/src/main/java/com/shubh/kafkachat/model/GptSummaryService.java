package com.shubh.kafkachat.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

// @Service
public class GptSummaryService {

    public static String runScript(String usrinput, String msginput) {
        ProcessBuilder processBuilder1 = new ProcessBuilder();
        ProcessBuilder processBuilder2 = new ProcessBuilder();
        // Assuming Python is in the system's PATH, and adjust the path to your script accordingly
        //processBuilder1.command("conda activate");
        processBuilder2.command("python", "./src/main/resources/summary.py", usrinput, msginput);

        try {
            //processBuilder1.start();
            Process process = processBuilder2.start();

            // Read the output of the script
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            // String summaryOut = "";
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                System.out.println("summary output:"+line);
                output.append(line).append("\n");
                // summaryOut = line;
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString();
                // return summaryOut;
            } else {
                // Handle error scenario
                return "Error executing script";
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
