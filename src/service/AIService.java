package service;

import util.EnvLoader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AIService {

    private static final String API_KEY = getApiKey();
    private static final String MODEL = getModel();
    private static final String API_URL = getApiUrl();
    private static final int CONNECT_TIMEOUT_MS = getIntEnv("NVIDIA_CONNECT_TIMEOUT_MS", 15000);
    private static final int READ_TIMEOUT_MS = getIntEnv("NVIDIA_READ_TIMEOUT_MS", 90000);
    private static final int MAX_RETRIES = getIntEnv("NVIDIA_MAX_RETRIES", 2);

    public static String askAI(String prompt) {

        if (API_KEY == null || API_KEY.isBlank()) {
            return "AI Error: Missing NVIDIA_API_KEY (or NVAPI_KEY) in environment";
        }

        int attempts = Math.max(1, MAX_RETRIES + 1);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                HttpResult result = invokeChatCompletions(prompt);
                if (result.status < 200 || result.status >= 300) {
                    return "AI Error: HTTP " + result.status + " - " + extractApiError(result.body);
                }
                return extractText(result.body);
            } catch (SocketTimeoutException e) {
                if (attempt == attempts) {
                    return "AI Error: Read timed out after " + attempts + " attempt(s). Increase NVIDIA_READ_TIMEOUT_MS if needed.";
                }
            } catch (Exception e) {
                return "AI Error: " + e.getMessage();
            }
        }

        return "AI Error: Request failed";
    }

    private static String getApiKey() {
        String key = EnvLoader.get("NVIDIA_API_KEY");
        if (key == null || key.isBlank()) {
            key = EnvLoader.get("NVAPI_KEY");
        }
        if (key == null || key.isBlank()) {
            key = EnvLoader.get("GEMINI_API_KEY");
        }
        return key;
    }

    private static String getModel() {
        String model = EnvLoader.get("NVIDIA_MODEL");
        if (model == null || model.isBlank()) {
            return "google/gemma-3n-e4b-it";
        }
        return model;
    }

    private static String getApiUrl() {
        String url = EnvLoader.get("NVIDIA_API_URL");
        if (url == null || url.isBlank()) {
            return "https://integrate.api.nvidia.com/v1/chat/completions";
        }
        return url;
    }

    private static HttpResult invokeChatCompletions(String prompt) throws IOException {
        URL url = URI.create(API_URL).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);

        String jsonInput = "{"
                + "\"model\":\"" + escapeJson(MODEL) + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}],"
            + "\"max_tokens\":512,"
                + "\"temperature\":1.0,"
                + "\"top_p\":0.95,"
                + "\"stream\":false"
                + "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int status = conn.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        StringBuilder response = new StringBuilder();
        if (stream != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
        }

        return new HttpResult(status, response.toString());
    }

    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String extractText(String json) {
        try {
            int marker = json.indexOf("\"content\":\"");
            if (marker < 0) {
                return "Could not parse AI response";
            }

            int start = marker + 11;
            int end = start;

            while (end < json.length()) {
                char current = json.charAt(end);
                if (current == '"' && json.charAt(end - 1) != '\\') {
                    break;
                }
                end++;
            }

            if (end >= json.length()) {
                return "Could not parse AI response";
            }

            return json.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        } catch (Exception e) {
            return "Could not parse AI response";
        }
    }

    private static String extractApiError(String json) {
        try {
            int marker = json.indexOf("\"message\":\"");
            if (marker < 0) {
                return fallbackApiError(json);
            }

            int start = marker + 11;
            int end = start;

            while (end < json.length()) {
                char current = json.charAt(end);
                if (current == '"' && json.charAt(end - 1) != '\\') {
                    break;
                }
                end++;
            }

            if (end >= json.length()) {
                return fallbackApiError(json);
            }

            return json.substring(start, end).replace("\\n", "\n");
        } catch (Exception e) {
            return fallbackApiError(json);
        }
    }

    private static String fallbackApiError(String json) {
        if (json == null || json.isBlank()) {
            return "Request rejected by API";
        }
        if (json.toLowerCase().contains("quota") || json.toLowerCase().contains("rate")) {
            return "Quota or rate limit exceeded for this API key/project";
        }
        String compact = json.replaceAll("\\s+", " ").trim();
        if (compact.length() > 160) {
            compact = compact.substring(0, 160) + "...";
        }
        return compact;
    }

    private static int getIntEnv(String key, int defaultValue) {
        String value = EnvLoader.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static final class HttpResult {
        private final int status;
        private final String body;

        private HttpResult(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }
}