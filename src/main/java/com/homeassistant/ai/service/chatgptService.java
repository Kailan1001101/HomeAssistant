package com.homeassistant.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;


import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class chatgptService {

        //Add API Key from OpenAPI
        //private static final String API_KEY = "";
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String sendMessage(String userMessage) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", "gpt-3.5-turbo");

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", userMessage)
            );
            body.put("messages", messages);

            String jsonBody = mapper.writeValueAsString(body);

            Request request = new Request.Builder()
                    .url(ENDPOINT)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    String responseBody = response.body().string();

                    System.out.println("Raw response: " + responseBody);
                    // Parse JSON to extract assistant message content
                    JsonNode root = mapper.readTree(responseBody);
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode messageNode = choices.get(0).path("message");
                        String assistantReply = messageNode.path("content").asText();
                        return assistantReply.trim();
                    } else {
                        return "No choices found in response";
                    }
                } else {
                    return "Empty response body";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}

