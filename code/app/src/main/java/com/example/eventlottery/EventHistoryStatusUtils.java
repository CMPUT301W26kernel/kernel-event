/**
 * Utility class
 * Determines the status of a user in an event's lottery system.
 * Last Modified: 2026-03-23 by Rebecca OluwaBiyi
 *
 * @author Rebecca OluwaBiyi
 * @since 2026-03-23
 */

package com.example.eventlottery;

import java.util.List;


public class EventHistoryStatusUtils {

    /**
     * Determines the status string for a user based on their presence in various lists of an event.
     *
     * @param userId        The ID of the user whose status is being determined.
     * @param waitingList   The list of users on the waiting list.
     * @param invitedList   The list of users invited to join the event.
     * @param acceptedList  The list of users who have accepted the invitation.
     * @param cancelledList The list of users who have declined or been cancelled.
     * @return A status string representing the user's lottery outcome.
     */
    public static String determineStatus(String userId, List<String> waitingList, List<String> invitedList,
                                        List<String> acceptedList, List<String> cancelledList) {
        if (acceptedList != null && acceptedList.contains(userId)) {
            return "Lottery results:\nSelected (Accepted)";
        } else if (cancelledList != null && cancelledList.contains(userId)) {
            return "Lottery results:\nSelected (Declined)";
        } else if (invitedList != null && invitedList.contains(userId)) {
            return "Lottery results:\nSelected (Waiting on your response)";
        } else if (waitingList != null && waitingList.contains(userId)) {
            // If anyone has been invited, accepted, or cancelled, a draw happened.
            // Therefore, anyone still in the waiting list was NOT selected.
            boolean drawHappened = (invitedList != null && !invitedList.isEmpty()) ||
                                   (acceptedList != null && !acceptedList.isEmpty()) ||
                                   (cancelledList != null && !cancelledList.isEmpty());
            
            if (drawHappened) {
                return "Lottery results:\nNot selected";
            } else {
                return "Lottery results:\nPending...";
            }
        }
        return "Unknown status";
    }
}
