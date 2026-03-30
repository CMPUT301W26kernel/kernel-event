/**
 * Event
 * A class representing an Event which stores event data
 * Last Modified: 2026-03-29 by Grace MacKenzie
 * <p>
 *     Notes:
 *     - the registration open and close MUST be ZonedDateTimes. The project specifications state that
 *       times need to be zoned, and we will need to perform a number of date comparisons for
 *       filtering and such.
 *     - Please do not touch registrationOpenIso, registrationCloseIso, encodedPosterImage.
 *       These are meant purely for storage in Firebase and are automatically encoded, decoded,
 *       and updated as needed.
 * </p>
 *
 * @author Grace MacKenzie
 * @since 2026-03-02
 */
package com.example.eventlottery;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.Exclude;

import java.io.ByteArrayOutputStream;
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
    private Bitmap posterImage = null;
    private String encodedPosterImage; // Read only

    @Exclude
    private ZonedDateTime registrationOpen = null;
    private String registrationOpenIso; // Read only

    @Exclude
    private ZonedDateTime registrationClose = null;
    private String registrationCloseIso; // Read only

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
     * A public getter required by Firestore to store encodedPosterImage
     * @return The encoded base 64 string associated with the poster image
     */
    public String getEncodedPosterImage() {
        return encodedPosterImage;
    }

    @Exclude
    public Bitmap getPosterImage() {
        if (this.posterImage == null) {
            // Decode string to bitmap: Base64 string -> Byte Array -> Bitmap
            byte[] bytes = Base64.decode(encodedPosterImage, Base64.DEFAULT);
            this.posterImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return posterImage;
    }

    public void setPosterImage(Bitmap posterImage) {
        this.posterImage = posterImage;

        // Encode bitmap as string: Bitmap -> Byte Array -> Base 64 string
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        posterImage.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        byte[] bytes = stream.toByteArray();
        this.encodedPosterImage = Base64.encodeToString(bytes, Base64.DEFAULT);
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
