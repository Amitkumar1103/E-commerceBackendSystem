package util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class EnvLoader {

    private static final Map<String, String> envMap = new HashMap<>();

    static {
        try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    envMap.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load .env file");
        }
    }

    public static String get(String key) {
        String value = envMap.get(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return System.getenv(key);
    }
}