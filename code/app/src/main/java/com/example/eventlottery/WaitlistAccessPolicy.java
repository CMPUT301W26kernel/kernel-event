package com.example.eventlottery;

/**
 * Centralized waitlist access checks used by the event overview and waitlist management UI.
 */
public final class WaitlistAccessPolicy {

    private WaitlistAccessPolicy() {
    }

    /**
     * Organizers/co-organizers for the event and admins can view/manage the waiting list.
     */
    public static boolean canManageWaitlist(String currentUserId, String currentUserRole, Event event) {
        if (currentUserId == null || currentUserId.trim().isEmpty() || event == null) {
            return false;
        }

        if ("admin".equalsIgnoreCase(currentUserRole)) {
            return true;
        }

        return event.isOrganizer(currentUserId);
    }
}
