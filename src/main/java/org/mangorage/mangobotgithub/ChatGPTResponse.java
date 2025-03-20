package org.mangorage.mangobotgithub;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class ChatGPTResponse {

    private String id;
    private String object;
    private int created;
    private String model;

    @SerializedName("choices")
    private List<Choice> choices;

    // Define inner class for choices
    public final static class Choice {
        private Message message;
        private int index;

        // Getters and setters
        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    // Define inner class for message
    public final static class Message {
        private String role;
        private String content;

        // Getters and setters
        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    // Getters and setters for top-level fields
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }
}