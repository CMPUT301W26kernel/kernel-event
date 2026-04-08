/**
 * Event Input
 * A small helper class to bundle Event Input and improve code readability.
 * Last Modified: 2026-03-10 by Grace MacKenzie
 *
 * @author Grace MacKenzie
 * @since 2026-03-10
 */
package com.example.eventlottery.creation;

/**
 * A helper class which holds raw user input data to be used as Event Input
 */
public class EventInput {
    public final String title;
    public final String description;
    public final String registrationOpen;
    public final String registrationClose;
    public final String waitingListCapacity;
    public final String venueAddress;
    public final String geolocationRadiusMeters;
    public final boolean requireGeolocationForWaitlist;
    public final String tagsRaw;

    public EventInput(
            String title,
            String description,
            String registrationOpen,
            String registrationClose,
            String waitingListCapacity,
            String venueAddress,
            String geolocationRadiusMeters,
            boolean requireGeolocationForWaitlist,
            String tagsRaw
    ) {
        this.title = title;
        this.description = description;
        this.registrationOpen = registrationOpen;
        this.registrationClose = registrationClose;
        this.waitingListCapacity = waitingListCapacity;
        this.venueAddress = venueAddress;
        this.geolocationRadiusMeters = geolocationRadiusMeters;
        this.requireGeolocationForWaitlist = requireGeolocationForWaitlist;
        this.tagsRaw = tagsRaw;
    }

}
