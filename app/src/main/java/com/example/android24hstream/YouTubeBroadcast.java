package com.example.android24hstream;

public class YouTubeBroadcast {
    private String id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String status;

    // Constructor
    public YouTubeBroadcast(String id, String title, String description, String thumbnailUrl, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getStatus() { return status; }
}