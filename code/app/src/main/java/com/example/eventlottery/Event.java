/**
 * Event
 * (description here)
 * Last Modified: 2026-03-10 by Grace MacKenzie
 *
 * @author Grace MacKenzie
 * @since 2026-03-02
 */
package com.example.eventlottery;
import androidx.annotation.Nullable;

import java.util.Date;

/**
 * A class representing an Event which stores event data
 */
public class Event {
    String eventId; // This is set inside the CreateEventFragment. Do not try to change or set it otherwise.
    String title;
    String description;
    String organizerId; // Read only.
    Date registrationOpen;
    Date registrationClose;
    Integer waitingListCapacity; // maybe -1 if there is no limit

    // Not sure how to do the image, but a reference to it should go in here.
    // Not sure how to do the QR code, but a reference should also go in here.
    // geological requirement thingy also goes somewhere in here. see US 02.02.03

    /**
     * An Event constructor
     * @param title The title of the event
     * @param description The description of the event
     * @param organizerId The Firestore id for the organizer of this event
     * @param registrationOpen the date the registration opens
     * @param registrationClose the date the registration closes
     */
    public Event(String title, String description, String organizerId, Date registrationOpen, Date registrationClose, @Nullable Integer waitingListCapacity) {
        // TODO: Generate a Firestore event ID
        this.title = title;
        this.description = description;
        this.organizerId = organizerId;
        this.registrationOpen = registrationOpen;
        this.registrationClose = registrationClose;

        // Check that waitingListCapacity is within acceptable range
        if (waitingListCapacity == null || waitingListCapacity > 0 ) {
            this.waitingListCapacity = waitingListCapacity;
        } else {
            throw new IllegalArgumentException("Waiting list capacity must be a positive number or null.");
        }
    }

    // GETTERS & SETTERS
    // TODO: Set conditions on setters for some attributes (namely the date attributes)

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public Date getRegistrationOpen() {
        return registrationOpen;
    }

    public void setRegistrationOpen(Date registrationOpen) {
        this.registrationOpen = registrationOpen;
    }

    public Date getRegistrationClose() {
        return registrationClose;
    }

    public void setRegistrationClose(Date registrationClose) {
        this.registrationClose = registrationClose;
    }

    public Integer getWaitingListCapacity() {
        return waitingListCapacity;
    }

    public void setWaitingListCapacity(@Nullable Integer waitingListCapacity) {
        if (waitingListCapacity == null || waitingListCapacity > 0 ) {
            this.waitingListCapacity = waitingListCapacity;
        } else {
            throw new IllegalArgumentException("Waiting list capacity must be a positive number or null.");
        }
    }

}
