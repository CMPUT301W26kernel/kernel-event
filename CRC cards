# System Classes

## EventFilter
**Responsibilities:**
- Filter events by interests
- Filter events by availability
- Filter events by date range
- Filter events by location
- Provide search functionality
- Support sorting options

**Collaborators:** Event

---

## Enrollment
**Responsibilities:**
- Track confirmed registrations for event
- Store enrollment timestamp
- Link user to event after acceptance
- Provide final attendee list
- Track attendance status
- Support cancellation by organizer/user

**Collaborators:** User, Event, Invitation

---

## Location
**Responsibilities:**
- Store geological coordinates when joining waitlist
- Associate location with user and event
- Support map visualizations for organizers
- Respect opt-out/disable settings per event
- Reanonymize location data
- Validate location accuracy

**Collaborators:** User, Event, WaitingList

---

## Profile
**Responsibilities:**
- Display user information screen
- Allow editing of personal details
- Show event history (won/lost/enrolled)
- Support profile deletion
- Manage notification preferences
- Display device ID identifier

**Collaborators:** User, Event, Enrollment

---

## Admin
**Responsibilities:**
- Browse and remove events
- Browse and remove profiles
- Browse and remove images
- Remove organizers violating policy
- Review notification logs
- Access system-wide stats
- Manage reported content

**Collaborators:** Event, User, Image, Notifications

---

## Notification
**Responsibilities:**
- Send notifications to users based on events
- Track notification type (won/lost/selected/cancelled/general)
- Store notification delivery status
- Respect user opt-out preferences
- Log all notifications sent
- Support bulk notifications to user groups
- Handle organization broadcast messages

**Collaborators:** User, Event, Lottery, Invitation, Admin

---

## Images
**Responsibilities:**
- Generate unique QR code for each event
- Store QR code data/payload
- Link QR code to event detail page
- Support scanning via device camera
- Validate QR code authenticity
- Handle QR code regeneration

**Collaborators:** Event

---

## QRCode
**Responsibilities:**
- Generate unique QR code for each event
- Store QR code data/payload
- Link QR code to event detail page
- Support scanning via device camera
- Validate QR code authenticity
- Handle QR code generation

**Collaborators:** Event

---

## Invitation
**Responsibilities:**
- Link selected user to event after lottery
- Track invitation status (pending/accepted/declined/expired)
- Store timestamp of invitation sent
- Store timestamp of user response
- Handle invitation expiration logic
- Trigger replacement draw on decline/expire

**Collaborators:** User, Event, Lottery, Notification

---

## User
**Responsibilities:**
- Store device ID + basic profile info (name/email/phone optional)
- Store role (entrant / organizer / admin)
- Store notification opt-in/out

**Collaborators:** Event, WaitingList, Invitation

---

## Event
**Responsibilities:**
- Store event details + registration open/close + capacity
- Link to organizer
- Expose event details page (what QR opens)

**Collaborators:** User, WaitingList, Lottery, Invitation, Enrollment, QRCode

---

## Waitlist
**Responsibilities:**
- Add/remove users for a given event
- Provide count of entrants
- Provide eligible pool for lottery draw

**Collaborators:** Event, User, Lottery

---

## Lottery
**Responsibilities:**
- Draw N winners from waitlist when registration closes
- Draw replacement when someone declines/cancels
- Ensure no duplicates + respect capacity

**Collaborators:** Event, WaitingList, Invitation, Enrollment
