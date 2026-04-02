/**
 * Validation Context
 * A class which works in conjunction with ValidationResult
 * Last Modified: 2026-03-10 by Grace MacKenzie
 *
 * @author Grace MacKenzie
 * @since 2026-03-10
 */
package com.example.eventlottery.creation;

import androidx.annotation.Nullable;

import com.example.eventlottery.Event;

/**
 * A class which bundles the information required by ValidationResult
 */
public class ValidationContext {
    public final EventCreationMode mode;
    public final EventInput input;
    @Nullable public final Event event;
    @Nullable public final String organizerId;

    public ValidationContext (EventCreationMode mode, EventInput input, @Nullable Event event, @Nullable String organizerId) {
        this.mode = mode;
        this.input = input;
        this.event = event;
        this.organizerId = organizerId;
    }

}
