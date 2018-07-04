package com.novamaday.ticketbird.objects.api;

public class UserAPIAccount {
    private String userId;
    private String APIKey;
    private boolean blocked;
    private long timeIssued;
    private int uses;

    //Getters
    public String getUserId() {
        return userId;
    }

    public String getAPIKey() {
        return APIKey;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public long getTimeIssued() {
        return timeIssued;
    }

    public int getUses() {
        return uses;
    }

    //Setters
    public void setUserId(String _userId) {
        userId = _userId;
    }

    public void setAPIKey(String _apiKey) {
        APIKey = _apiKey;
    }

    public void setBlocked(boolean _blocked) {
        blocked = _blocked;
    }

    public void setTimeIssued(long _time) {
        timeIssued = _time;
    }

    public void setUses(int _uses) {
        uses = _uses;
    }
}