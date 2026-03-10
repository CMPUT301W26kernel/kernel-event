/**
 * Validation Result
 * A small helper class to assist in user input validation.
 * Last Modified: 2026-03-010 by Grace MacKenzie
 *
 * @author Grace MacKenzie
 * @since 2026-03-10
 */
package com.example.eventlottery;

import androidx.annotation.Nullable;

public class ValidationResult {
    public final boolean isValid;
    public final String errorMessage;
    public final Event event; // This event holds the parsed input and placeholders.

    public ValidationResult(boolean isValid, @Nullable String errorMessage, Event event) {
        this.isValid = isValid;
        this.errorMessage = errorMessage;
        this.event = event;
    }
}
