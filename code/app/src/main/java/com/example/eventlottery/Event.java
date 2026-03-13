package com.example.eventlottery;

/**
 * Event model class representing one event in the app.
 * This is used for displaying event information on the home page
 * and event overview page.
 */
public class Event {

    private String title;
    private String description;
    private String organizerName;
    private String startDate;
    private String registrationDeadline;
    private int maxParticipants;

    public Event() {
        // Required empty constructor
    }

    public Event(String title, String description, String organizerName,
                 String startDate, String registrationDeadline, int maxParticipants) {
        this.title = title;
        this.description = description;
        this.organizerName = organizerName;
        this.startDate = startDate;
        this.registrationDeadline = registrationDeadline;
        this.maxParticipants = maxParticipants;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getRegistrationDeadline() {
        return registrationDeadline;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setRegistrationDeadline(String registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
}