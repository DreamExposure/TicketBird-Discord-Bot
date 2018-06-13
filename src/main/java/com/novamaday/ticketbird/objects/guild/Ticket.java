package com.novamaday.ticketbird.objects.guild;

public class Ticket {
    private final int number;
    private String project;
    private long creator;
    private long channel;
    private long category;

    public Ticket(int _number) {
        number = _number;
        project = "N/a";
    }

    //Getters
    public int getNumber() {
        return number;
    }

    public String getProject() {
        return project;
    }

    public long getCreator() {
        return creator;
    }

    public long getChannel() {
        return channel;
    }

    public long getCategory() {
        return category;
    }

    //Setters
    public void setProject(String _project) {
        project = _project;
    }

    public void setCreator(long _creator) {
        creator = _creator;
    }

    public void setChannel(long _channel) {
        channel = _channel;
    }

    public void setCategory(long _category) {
        category = _category;
    }
}