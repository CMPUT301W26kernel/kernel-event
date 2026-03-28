package com.example.eventlottery;

/**
 * Pure business rules for event comment permissions and pinning.
 */
public final class EventCommentPolicy {

    private EventCommentPolicy() {
    }

    /**
     * Returns whether the current viewer is allowed to create a comment on the event.
     *
     * @param currentUserId Signed-in user id, if any.
     * @param currentUserRole Viewer role from the user profile.
     * @param organizerId Owner of the event.
     * @return True when the viewer is an entrant or the organizer of the event.
     */
    public static boolean canPostComment(String currentUserId, String currentUserRole, String organizerId) {
        if (currentUserId == null || currentUserRole == null) {
            return false;
        }

        if ("admin".equalsIgnoreCase(currentUserRole)) {
            return false;
        }

        if ("organizer".equalsIgnoreCase(currentUserRole)) {
            return currentUserId.equals(organizerId);
        }

        return "entrant".equalsIgnoreCase(currentUserRole);
    }

    /**
     * Returns whether a comment should be pinned because it was authored by the event organizer.
     *
     * @param authorId Comment author id.
     * @param organizerId Event organizer id.
     * @return True when the author owns the event.
     */
    public static boolean shouldPinComment(String authorId, String organizerId) {
        return authorId != null && authorId.equals(organizerId);
    }

    /**
     * Returns whether the current viewer can remove the given comment.
     *
     * @param comment Comment being evaluated.
     * @param currentUserId Signed-in user id, if any.
     * @param currentUserRole Viewer role from the user profile.
     * @param organizerId Owner of the event.
     * @return True for admins and for organizers removing other users' comments on their event.
     */
    public static boolean canDeleteComment(
            EventComment comment,
            String currentUserId,
            String currentUserRole,
            String organizerId
    ) {
        if (comment == null || currentUserId == null || currentUserRole == null || comment.hasBeenRemoved()) {
            return false;
        }

        if ("admin".equalsIgnoreCase(currentUserRole)) {
            return true;
        }

        boolean isOrganizerOfEvent = currentUserId.equals(organizerId)
                && "organizer".equalsIgnoreCase(currentUserRole);

        return isOrganizerOfEvent && !currentUserId.equals(comment.getAuthorId());
    }
}
