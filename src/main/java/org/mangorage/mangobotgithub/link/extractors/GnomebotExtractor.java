package org.mangorage.mangobotgithub.link.extractors;

import org.mangorage.mangobotgithub.link.LinkExtractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

public final class GnomebotExtractor implements LinkExtractor {
    @Override
    public Optional<String> fetch(String url) {
        if (url.contains("gnomebot.dev")) {
            var index = url.lastIndexOf("/");
            var id = url.substring(index + 1);
            var data = fetchData("https://api.mclo.gs/1/raw/" + id);
            return data == null || data.isBlank() ? Optional.empty() : Optional.of(data);
        }
        return Optional.empty();
    }

    public static String fetchData(String urlString) {
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result.toString();
    }
}
