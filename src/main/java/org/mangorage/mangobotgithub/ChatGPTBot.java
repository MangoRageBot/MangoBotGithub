package org.mangorage.mangobotgithub;
import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;

public final class ChatGPTBot {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final Gson gson = new Gson();

    public static ChatGPTResponse askChatGPT(String prompt) throws IOException {
        OkHttpClient client = new OkHttpClient();

        String json = "{ \"model\": \"gpt-4o-mini\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}] }";

        RequestBody body = RequestBody.create(MediaType.get("application/json"), json);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + MangoBotGithub.CHAT_AI_TOKEN.get())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                return gson.fromJson(responseBody, ChatGPTResponse.class);
            }
        }
        return null;
    }
}
