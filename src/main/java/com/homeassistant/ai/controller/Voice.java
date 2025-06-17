package com.homeassistant.ai.controller;
import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;


public class Voice {
    private static final String AUDIO_FILE_PATH = "audio_input.wav";
    private static final String PYTHON_STT_URL = "http://localhost:5005/stt";
    private static final String CHATGPT_URL = "http://localhost:8080/chat";
    private static final String PYTHON_TTS_URL = "http://localhost:5005/tts";
    private static final int RECORD_TIME = 5000; // milliseconds

    public static void start() throws Exception {
        System.out.println("Recording...");
        recordAudio(AUDIO_FILE_PATH, RECORD_TIME);

        System.out.println("Sending audio to STT service...");
        String transcript = sendAudioToPython(PYTHON_STT_URL, AUDIO_FILE_PATH);
        System.out.println("Transcript: " + transcript);

        System.out.println("Sending to ChatGPT...");
        String responseText = sendTextToPython(CHATGPT_URL, transcript);
        System.out.println("ChatGPT: " + responseText);

        System.out.println("Requesting TTS audio...");
        String ttsAudioPath = sendTextToPython(PYTHON_TTS_URL, responseText);

        System.out.println("Playing response...");
        playAudio(ttsAudioPath);
    }

    private static void recordAudio(String filePath, int milliseconds) throws Exception {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        AudioInputStream ais = new AudioInputStream(line);
        File wavFile = new File(filePath);
        Thread stopper = new Thread(() -> {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException ignored) {}
            line.stop();
            line.close();
        });

        stopper.start();
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
    }

    private static String sendAudioToPython(String endpoint, String filePath) throws IOException {
        return sendFileToEndpoint(endpoint, new File(filePath));
    }

    private static String sendTextToPython(String endpoint, String text) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        System.out.println(text);
        String jsonPayload = String.format("{\"content\":\"%s\"}", text);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes());
        }

        return new String(conn.getInputStream().readAllBytes());
    }

    private static String sendFileToEndpoint(String endpoint, File file) throws IOException {
        String boundary = Long.toHexString(System.currentTimeMillis());
        String LINE_FEED = "\r\n";

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream output = conn.getOutputStream(); PrintWriter writer = new PrintWriter(new OutputStreamWriter(output))) {
            // File part
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"audio\"; filename=\"" + file.getName() + "\"").append(LINE_FEED);
            writer.append("Content-Type: audio/wav").append(LINE_FEED);
            writer.append(LINE_FEED).flush();
            Files.copy(file.toPath(), output);
            output.flush();
            writer.append(LINE_FEED).flush();

            // End of multipart
            writer.append("--" + boundary + "--").append(LINE_FEED).flush();
        }

        return new String(conn.getInputStream().readAllBytes());
    }

    private static void playAudio(String path) throws Exception {
        File audioFile = new File(path);
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(audioFile);
        Clip clip = AudioSystem.getClip();
        clip.open(audioIn);
        clip.start();
        Thread.sleep((int) clip.getMicrosecondLength() / 1000);
        clip.close();
    }
}
