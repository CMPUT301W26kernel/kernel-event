/**
 * Validation Result
 * A small helper class to assist in user input validation.
 * Last Modified: 2026-03-10 by Grace MacKenzie
 *
 * @author Grace MacKenzie
 * @since 2026-03-10
 */
package com.example.eventlottery;

import androidx.annotation.Nullable;

public class ValidationResult {
    public final boolean isValid;
    @Nullable final String errorMessage;
    @Nullable final Event event; // This event holds the parsed input and placeholders.

    public ValidationResult(boolean isValid, @Nullable String errorMessage, @Nullable Event event) {
        this.isValid = isValid;
        this.errorMessage = errorMessage;
        this.event = event;
    }

    public static ValidationResult valid(Event event) {
        return new ValidationResult(true, null, event);
    }

    public static ValidationResult invalid(String errorMessage) {
        return new ValidationResult(false, errorMessage, null);
    }
}
