/**
 * Event
 * A class representing an Event which stores event data
 * Last Modified: 2026-03-21 by Grace MacKenzie
 * <p>
 *     Notes:
 *     - the registration open and close MUST be ZonedDateTimes. The project specifications state that
 *       times need to be zoned, and we will need to perform a number of date comparisons for
 *       filtering and such.
 * </p>
 *
 * @author Grace MacKenzie
 * @since 2026-03-02
 */
package com.example.eventlottery.event;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.Exclude;

import java.time.ZonedDateTime;

/**
 * A class representing an Event which stores event data
 */
public class Event {
    private String eventId; // This is set inside the CreateEventFragment
    private String title;
    private String description;
    private String organizerId; // Read only
    private Integer waitingListCapacity;

    @Exclude
    private ZonedDateTime registrationOpen = null;
    private String registrationOpenIso; // Read only

    @Exclude
    private ZonedDateTime registrationClose = null;
    private String registrationCloseIso; // Read only


    // Not sure how to do the image, but a reference to it should go in here.
    // Not sure how to do the QR code, but a reference should also go in here.
    // geological requirement thingy also goes somewhere in here. see US 02.02.03

    /**
     * An empty public constructor required for Firebase deserialization.
     * DO.NOT.DELETE.
     */
    public Event() {}

    /**
     * An Event constructor
     * @param title The title of the event
     * @param description The description of the event
     * @param organizerId The Firestore id for the organizer of this event
     * @param registrationOpen the date the registration opens
     * @param registrationClose the date the registration closes
     */
    public Event(String title, String description, String organizerId, ZonedDateTime registrationOpen, ZonedDateTime registrationClose, @Nullable Integer waitingListCapacity) {
        // TODO: Generate a Firestore event ID
        this.title = title;
        this.description = description;
        this.organizerId = organizerId;
        this.registrationOpenIso = registrationOpen.toString();
        this.registrationOpen = registrationOpen;
        this.registrationCloseIso = registrationClose.toString();
        this.registrationClose = registrationClose;

        // Check that waitingListCapacity is within acceptable range
        if (waitingListCapacity == null || waitingListCapacity > 0 ) {
            this.waitingListCapacity = waitingListCapacity;
        } else {
            throw new IllegalArgumentException("Waiting list capacity must be a positive number or null.");
        }
    }

    // GETTERS & SETTERS
    // TODO: Set conditions on setters for some attributes (namely the ZonedDateTime attributes)

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

    /**
     * A public getter required by Firestore to store RegistrationOpenIso
     * @return The Iso string associated with registrationOpen
     */
    public String getRegistrationOpenIso() {
        return registrationOpenIso;
    }

    @Exclude
    public ZonedDateTime getRegistrationOpen() {
        if (registrationOpen == null) {
            registrationOpen = ZonedDateTime.parse(registrationOpenIso);
        }
        return registrationOpen;
    }

    public void setRegistrationOpen(ZonedDateTime registrationOpen) {
        this.registrationOpen = registrationOpen;
        this.registrationOpenIso = registrationOpen.toString();
    }

    /**
     * A public getter required by Firestore to store RegistrationCloseIso
     * @return The Iso string associated with registrationClose
     */
    public String getRegistrationCloseIso() {
        return registrationCloseIso;
    }

    @Exclude
    public ZonedDateTime getRegistrationClose() {
        if (registrationClose == null) {
            registrationClose = ZonedDateTime.parse(registrationCloseIso);
        }
        return registrationClose;
    }

    public void setRegistrationClose(ZonedDateTime registrationClose) {
        this.registrationClose = registrationClose;
        this.registrationCloseIso = registrationClose.toString();
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
