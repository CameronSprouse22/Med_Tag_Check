# Feature Specification: NFC Medication Reminder System

**Feature Branch**: `001-nfc-med-reminder`  
**Created**: 2026-02-14  
**Status**: Draft  
**Input**: User description: "I want to create a simple android application using Java that uses NFC tags attached to medication bottles to help people with cognitive issues take their medication correctly and track refills."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - NFC Scan & Dose Confirmation (Priority: P1)

A user with cognitive issues hears their medication alarm. They navigate to the app and tap their phone on the NFC tag attached to the medication bottle. The app provides audible confirmation of the correct medication name, the dose amount to take, and announces the time until the next dose. The timer automatically resets for that medication.

**Why this priority**: This is the core safety feature that prevents medication errors. Users with cognitive issues need clear, audible confirmation to ensure they're taking the correct medication at the correct dose. Without this, the application provides no value.

**Independent Test**: Can be fully tested by creating a medication entry, writing to an NFC tag, setting an alarm, then scanning the tag to receive audio confirmation. Delivers immediate value by reducing medication confusion and errors.

**Acceptance Scenarios**:

1. **Given** user has an active medication with an NFC tag assigned, **When** user taps phone on the NFC tag, **Then** app speaks the medication nickname, dose amount, and time until next dose
2. **Given** user scans an NFC tag for a scheduled medication, **When** scan is successful, **Then** the medication timer resets to the configured schedule
3. **Given** user scans an NFC tag, **When** the tag is not recognized, **Then** app provides clear audio error message saying "Tag not recognized"
4. **Given** user scans the correct medication's NFC tag, **When** it's not time for that dose yet, **Then** app announces "Not time for [medication] yet, next dose in [X hours/minutes]"

---

### User Story 2 - Add Medication with Basic Info (Priority: P1)

A user needs to add a new medication to the system. They tap "Add Medication" from the main menu and enter essential information: category of importance (Life Dependent/Very Important/Beneficial), medication nickname, dose amount, starting number of remaining doses, and schedule (either every X hours or specific times of day). After entering all information, they are prompted to scan an NFC tag to link it to this medication.

**Why this priority**: Users cannot use the NFC scanning feature without first adding medications to the system. This is a prerequisite for all other functionality and represents the minimum setup needed for an MVP.

**Independent Test**: Can be fully tested by adding a medication with all required fields, writing to an NFC tag, then verifying the medication appears in the main list. Delivers value by enabling medication tracking.

**Acceptance Scenarios**:

1. **Given** user taps "Add Medication", **When** user completes all required fields and scans a tag, **Then** medication appears on the main menu list with all entered information
2. **Given** user is adding a medication, **When** user selects "every X hours" schedule, **Then** system calculates next dose times automatically based on first dose
3. **Given** user is adding a medication, **When** user selects "specific times of day" schedule, **Then** user can enter multiple time entries (e.g., 8:00 AM, 2:00 PM, 8:00 PM)
4. **Given** user is adding a medication with an end date, **When** user toggles "end period" ON, **Then** user can select an end date after which alarms will stop
5. **Given** user is entering medication info, **When** user attempts to proceed without required fields, **Then** system highlights missing fields and prevents proceeding

---

### User Story 3 - Medication List View & Status (Priority: P1)

Users open the app to see their main medication list. Each entry shows: time until next dose (or "Overdue" if missed), medication nickname, dose amount, remaining doses, and three buttons ("Med Refilled", "Info", "Write Tag"). The list is ordered by next dose time, with active medications first and inactive medications grayed out at the bottom.

**Why this priority**: Users need to see their medication status at a glance to know which medications are due, overdue, or running low. This provides essential situational awareness for managing multiple medications.

**Independent Test**: Can be fully tested by adding multiple medications with different schedules and dose counts, then verifying the display shows correct countdown timers, ordering, and status indicators. Delivers value through clear overview of all medications.

**Acceptance Scenarios**:

1. **Given** user has multiple active medications, **When** user opens the app, **Then** medications are sorted by next dose time (soonest first)
2. **Given** a medication dose time has passed without confirmation, **When** user views the list, **Then** that medication shows "Overdue" with time elapsed since missed dose
3. **Given** user has taken a dose, **When** user returns to main list, **Then** "Time Until Next Dose" shows countdown to next scheduled time
4. **Given** user has inactive medications, **When** user views the list, **Then** inactive entries appear last and are grayed out
5. **Given** user taps "Med Refilled" button, **When** button is pressed, **Then** remaining doses resets to configured maximum amount

---

### User Story 4 - Scheduled Alarms & Notifications (Priority: P2)

Users configure alarm preferences for each medication. Three alarm types are available: (1) alarm leading up to med time (e.g., 5 minutes before), (2) alarm when time is up (exact dose time), (3) alarm if medication has not been taken after dose time. Each medication category (Life Dependent/Very Important/Beneficial) can have different alarm sounds. When an alarm triggers, users can either scan the NFC tag to confirm or manually tap "Confirm med taken" or "Medication skipped" buttons (if that option is enabled).

**Why this priority**: For users with cognitive issues, timely reminders are critical for medication adherence. Without alarms, users may forget doses entirely. This is essential for the application's core purpose but can work independently of advanced features.

**Independent Test**: Can be fully tested by configuring a medication with specific alarm settings, waiting for alarm times, and verifying alarms trigger with correct sounds. Can also test manual confirmation vs. NFC scan confirmation.  Delivers value through improved medication adherence.

**Acceptance Scenarios**:

1. **Given** user has configured a "leading up" alarm for 5 minutes before dose time, **When** that time arrives, **Then** alarm plays with the configured sound for that medication category
2. **Given** an alarm has triggered, **When** user scans the correct NFC tag, **Then** alarm stops and dose is marked as taken
3. **Given** manual confirmation is enabled, **When** alarm triggers, **Then** user sees "Confirm med taken" and "Medication skipped" buttons
4. **Given** user taps "Medication skipped", **When** button is pressed, **Then** dose is logged as skipped and timer resets to next dose
5. **Given** medication has not been taken 30 minutes after dose time, **When** "missed dose" alarm is enabled, **Then** persistent alarm triggers until user responds
6. **Given** user has configured different sounds for each category, **When** alarms trigger for different medications, **Then** each plays its category-assigned sound

---

### User Story 5 - Low Dose Notifications & Refill Reminders (Priority: P2)

Users configure two dose thresholds for refill reminders: a notification threshold and an alarm threshold. When remaining doses fall below the first threshold, a notification is sent to the user. When doses fall below the second (lower) threshold, an alarm is set to trigger at a configured time (e.g., 9 AM the next day) to remind the user to refill the medication.

**Why this priority**: Running out of medication can have serious health consequences, especially for "Life Dependent" medications. Proactive refill reminders prevent gaps in medication availability and improve health outcomes.

**Independent Test**: Can be fully tested by setting low and critical thresholds, marking doses as taken until thresholds are reached, and verifying notifications and alarms trigger appropriately. Delivers value by preventing medication stockouts.

**Acceptance Scenarios**:

1. **Given** user has configured first notification threshold at 7 doses, **When** remaining doses drops to 7, **Then** user receives notification "Low on [medication] - 7 doses remaining"
2. **Given** user has configured second alarm threshold at 3 doses, **When** remaining doses drops to 3, **Then** system schedules alarm for configured refill reminder time
3. **Given** refill alarm time arrives, **When** alarm triggers, **Then** alarm announces "Time to refill [medication] - only 3 doses remaining"
4. **Given** user taps "Med Refilled" button, **When** button is pressed, **Then** remaining doses resets and all refill notifications/alarms are cleared
5. **Given** user configures different thresholds per medication category, **When** creating new medications, **Then** thresholds are auto-populated based on category defaults

---

### User Story 6 - Emergency Contact Notifications (Priority: P3)

Users configure emergency contact methods (text, call, email) and thresholds for each medication or category. When a user misses or skips a dose of a critical medication (typically "Life Dependent" category), the system sends a notification to the configured contact(s) with: medication name, whether it was missed or skipped, current device GPS location, and total number of missed doses for that medication.

**Why this priority**: This provides a safety net for users with severe cognitive issues who have caregivers. However, it's not essential for the core medication tracking functionality and can be added after basic features are working.

**Independent Test**: Can be fully tested by configuring emergency contacts, simulating missed doses, and verifying notifications are sent with correct information. Delivers value by providing caregiver oversight for critical medications.

**Acceptance Scenarios**:

1. **Given** user has configured emergency contact email for "Life Dependent" medications, **When** user skips a Life Dependent dose, **Then** email is sent to configured contact with medication name, "skipped" status, GPS location, and missed dose count
2. **Given** user has configured emergency text and call, **When** Life Dependent medication is missed by 2 hours, **Then** text is sent first, followed by call if configured
3. **Given** user has accumulated 2 missed doses of a medication, **When** third dose is missed, **Then** notification includes "3 missed doses total" in the message
4. **Given** user has not granted location permissions, **When** emergency notification is triggered, **Then** notification includes "Location unavailable" instead of GPS coordinates
5. **Given** user configures different contact methods per category, **When** "Very Important" medication is missed, **Then** only contacts configured for that category are notified

---

### User Story 7 - Write to NFC Tag (Priority: P3)

Users need to write medication information to a new NFC tag. From a medication's info screen, they tap "Write Tag" button. The app enters write mode with a fullscreen prompt saying "Hold phone near tag to write" and a Cancel button. The app writes a unique string ID to the tag (does not include medication name for privacy). Once write is successful, the tag is linked to that medication.

**Why this priority**: While useful for replacing lost/damaged tags or creating backup tags, this is not essential for initial use since tags are written during the "Add Medication" flow. This is a convenience feature for advanced users.

**Independent Test**: Can be fully tested by creating a medication, writing to multiple NFC tags, and verifying all tags correctly identify the same medication when scanned. Delivers value by enabling tag replacement and backups.

**Acceptance Scenarios**:

1. **Given** user taps "Write Tag" from medication info screen, **When** app enters write mode, **Then** screen shows "Hold phone near tag to write" with Cancel button
2. **Given** app is in write mode, **When** user holds phone near blank NFC tag, **Then** app writes unique ID and confirms "Tag written successfully"
3. **Given** user writes to multiple tags for same medication, **When** user scans any of those tags later, **Then** all tags correctly identify the same medication
4. **Given** user attempts to write to a tag already in use, **When** write is attempted, **Then** app displays warning "Tag already assigned to [other medication]" and asks for confirmation to overwrite
5. **Given** NFC write fails due to tag error, **When** failure occurs, **Then** app displays "Write failed, please try again or use different tag"

---

### User Story 8 - Settings & Customization (Priority: P3)

Users access Settings from the main menu to configure global preferences: sound volume, per-category alarm sounds (Life Dependent/Very Important/Beneficial), emergency contact information (email addresses and phone numbers), refill threshold defaults per category, and alarm configuration defaults. Users can also enable/disable the manual confirmation option (allowing "Confirm med taken" button instead of requiring NFC scan).

**Why this priority**: While customization improves user experience, the application can function with default settings. This is important for long-term usability but not critical for initial deployment.

**Independent Test**: Can be fully tested by changing various settings, then verifying those settings are applied to alarms, sounds, and new medications. Delivers value through personalization and accessibility improvements.

**Acceptance Scenarios**:

1. **Given** user opens Settings, **When** user adjusts master volume slider, **Then** all app sounds (alarms, confirmations) play at adjusted volume
2. **Given** user selects custom alarm sound for "Life Dependent" category, **When** Life Dependent medication alarm triggers, **Then** alarm plays selected sound
3. **Given** user enters emergency contact email and phone, **When** user saves settings, **Then** these contacts are used for all medications unless overridden at medication level
4. **Given** user sets default refill thresholds (e.g., 10 and 5 for Life Dependent), **When** user adds new Life Dependent medication, **Then** those thresholds are pre-filled
5. **Given** user disables manual confirmation option, **When** medication alarm triggers, **Then** only NFC scan can dismiss alarm (no manual buttons shown)
6. **Given** user enables manual confirmation option, **When** medication alarm triggers, **Then** "Confirm med taken" and "Medication skipped" buttons are displayed

---

### User Story 9 - Edit & Delete Medications (Priority: P3)

From a medication's Info screen, users can tap "Edit" to modify any medication details (nickname, dose, schedule, thresholds, category). Users can also delete a medication, which triggers a confirmation dialog. After confirming deletion, the app asks if the medication should be saved as "inactive". Inactive medications remain in the list for historical tracking but no longer trigger alarms.

**Why this priority**: While some editing capability is helpful, this is primarily for correcting errors or handling medication changes. Most users will set up medications once and leave them. This enhances usability but isn't core functionality.

**Independent Test**: Can be fully tested by creating a medication, editing its details, verifying changes persist, then deleting it and checking inactive status. Delivers value through flexibility in medication management.

**Acceptance Scenarios**:

1. **Given** user taps "Edit" from medication Info screen, **When** edit screen opens, **Then** all current medication fields are pre-filled and editable
2. **Given** user modifies medication schedule from "every 8 hours" to specific times, **When** user saves changes, **Then** new schedule takes effect for next dose
3. **Given** user taps "Delete" button, **When** button is pressed, **Then** confirmation dialog appears asking "Delete [medication]?"
4. **Given** user confirms deletion, **When** confirmation is accepted, **Then** app asks "Save as inactive? (Keeps history but stops alarms)"
5. **Given** user chooses to save as inactive, **When** option is selected, **Then** medication moves to bottom of list, is grayed out, and all alarms are disabled
6. **Given** user chooses not to save as inactive, **When** option is declined, **Then** medication and all associated data are permanently deleted

---

### User Story 10 - Read Tag Quick Access (Priority: P2)

Users tap "Read Tag" from the main menu. A fullscreen modal appears with "Scan tag now" message and a Cancel button. User holds phone near any configured NFC tag. Upon successful scan, the app immediately navigates to the Info screen for that medication, showing full details, edit/delete buttons, and current status.

**Why this priority**: This provides quick access to medication information without navigating through the main list, which is helpful when users need information about a specific medication bottle they're holding. It's a quality-of-life feature that enhances usability.

**Independent Test**: Can be fully tested by tapping Read Tag, scanning various medication tags, and verifying navigation to correct Info screens. Delivers value through faster access to medication details.

**Acceptance Scenarios**:

1. **Given** user taps "Read Tag" from main menu, **When** button is pressed, **Then** fullscreen modal appears with "Scan tag now" text and Cancel button
2. **Given** scan modal is open, **When** user holds phone near configured NFC tag, **Then** app dismisses modal and navigates to that medication's Info screen
3. **Given** scan modal is open, **When** user scans unrecognized tag, **Then** app shows error "Tag not found in system" but keeps modal open to try again
4. **Given** scan modal is open, **When** user taps Cancel, **Then** modal closes and user returns to main menu
5. **Given** user scans tag from Read Tag modal, **When** medication Info screen appears, **Then** screen shows all medication details, remaining doses, next dose time, and Edit/Delete buttons

---

### Edge Cases

- What happens when user scans an NFC tag while phone NFC is disabled? (System detects and shows error "NFC is disabled, please enable in phone settings")
- What happens when user's phone does not have NFC hardware? (App detects during startup and operates in "manual mode" without scanning capability, showing informational message)
- What happens when user tries to add medication but cancels NFC tag scan? (Medication is saved but marked as "No tag assigned", user can assign later via Write Tag)
- What happens when multiple alarms trigger simultaneously? (Alarms are queued and presented one at a time, prioritized by category: Life Dependent > Very Important > Beneficial)
- What happens when user takes phone out of range during NFC write operation? (Write fails gracefully with error message, tag state is not corrupted, user can retry)
- What happens when user changes phone time/timezone? (All scheduled times adjust to new timezone, app uses absolute timestamps internally)
- What happens when user runs app for first time with no medications? (Welcome screen explains app purpose and guides to "Add Medication" button)
- What happens when emergency notification fails to send (no network/invalid email)? (System logs failure, shows notification to user, and retries on next missed dose)
- What happens when user disables app permissions for location? (App functions normally but location field in emergency notifications shows "Location unavailable")
- What happens when remaining doses reaches zero? (Continuous refill alarms trigger daily until user marks as refilled, medication shows "OUT OF DOSES" warning)

## Requirements *(mandatory)*

### Functional Requirements

**NFC Tag Management**
- **FR-001**: System MUST read NFC tags using Android NFC API (NDEF format) and retrieve associated medication ID
- **FR-002**: System MUST write unique medication IDs to NFC tags without including medication name or sensitive information
- **FR-003**: System MUST handle NFC read/write failures gracefully with clear error messages to user
- **FR-004**: System MUST support multiple NFC tags linked to the same medication (for backup tags)
- **FR-005**: System MUST detect when phone NFC is disabled and prompt user to enable it

**Medication Data Management**
- **FR-006**: System MUST store medication information including: nickname, dose, remaining dose count, category (Life Dependent/Very Important/Beneficial), and schedule
- **FR-007**: System MUST support two scheduling modes: "every X hours" and "specific times of day"
- **FR-008**: System MUST allow optional end date for medication schedules
- **FR-009**: System MUST persist all medication data locally on device (no cloud storage by default)
- **FR-010**: System MUST maintain dose history (taken, skipped, missed) for each medication
- **FR-011**: Users MUST be able to edit any medication field after creation
- **FR-012**: Users MUST be able to delete medications with confirmation dialog
- **FR-013**: System MUST support "inactive" medication status that preserves history but disables alarms

**Dose Confirmation & Tracking**
- **FR-014**: System MUST accept dose confirmation via NFC tag scan or manual button press (if enabled)
- **FR-015**: System MUST provide audible confirmation including: medication nickname, dose amount, time until next dose
- **FR-016**: System MUST automatically decrement remaining dose count when dose is confirmed as taken
- **FR-017**: System MUST reset medication timer to next scheduled dose time after confirmation
- **FR-018**: System MUST allow users to mark dose as "skipped" separate from "taken"
- **FR-019**: System MUST track count of consecutive missed doses per medication
- **FR-020**: System MUST prevent confirming doses before scheduled time (with override option for flexibility)

**Alarm & Notification System**
- **FR-021**: System MUST support three alarm types per medication: pre-dose (X minutes before), on-time (at exact dose time), and post-dose (X minutes after if not taken)
- **FR-022**: System MUST allow configuration of different alarm sounds per medication category
- **FR-023**: System MUST use Android AlarmManager to ensure alarms trigger even when app is closed
- **FR-024**: System MUST display alarm notification with options to open app or snooze
- **FR-025**: System MUST handle multiple simultaneous alarms by queueing and prioritizing by category
- **FR-026**: System MUST persist through device reboot by re-scheduling all active alarms on boot

**Refill Tracking**
- **FR-027**: System MUST send notification when remaining doses fall below configured threshold (first warning level)
- **FR-028**: System MUST schedule persistent alarm at configured time when doses fall below second threshold (critical level)
- **FR-029**: Users MUST be able to quickly mark medication as "refilled" which resets dose count to configured maximum
- **FR-030**: System MUST clear all refill notifications/alarms when medication is marked as refilled
- **FR-031**: System MUST support different refill thresholds per medication or category

**Emergency Contact Notifications**
- **FR-032**: System MUST send notifications to configured contacts (email, SMS, phone call) when critical medications are missed
- **FR-033**: Emergency notifications MUST include: medication name, missed/skipped status, GPS location (if available), total missed dose count
- **FR-034**: Users MUST be able to configure emergency contacts per medication or per category
- **FR-035**: System MUST support configurable thresholds for triggering emergency notifications (e.g., after 1 missed dose, 2 missed doses, etc.)
- **FR-036**: System MUST handle notification failures gracefully and retry on next missed dose
- **FR-037**: System MUST respect user privacy by only sending emergency notifications when explicitly configured

**User Interface Requirements**
- **FR-038**: Main menu MUST display all active medications sorted by next dose time (soonest first)
- **FR-039**: Each medication entry MUST show: time until next dose (or "Overdue"), nickname, dose, remaining doses
- **FR-040**: Main menu MUST have three bottom navigation buttons: "Settings", "Read Tag", "Add Medication"
- **FR-041**: Medication Info screen MUST have "Edit", "Delete", "Write Tag", and "Change Remaining Amount" buttons
- **FR-042**: "Read Tag" button MUST trigger fullscreen NFC scan modal with Cancel button
- **FR-043**: Add Medication flow MUST prompt for all required fields before allowing NFC tag assignment
- **FR-044**: Settings screen MUST provide controls for: alarm sounds, volume, emergency contacts, refill thresholds, manual confirmation toggle
- **FR-045**: Inactive medications MUST appear at bottom of list in grayed-out state
- **FR-046**: System MUST provide clear visual indicators for overdue medications (e.g., red highlight, warning icon)

**Audio Feedback Requirements**
- **FR-047**: System MUST provide text-to-speech audio confirmation for: medication name, dose amount, time until next dose
- **FR-048**: System MUST play distinct alarm sounds based on medication category
- **FR-049**: Audio volume MUST be controllable via Settings screen
- **FR-050**: System MUST respect Android system volume settings and do-not-disturb mode

**Data Validation & Safety**
- **FR-051**: System MUST require confirmation dialogs for destructive actions (delete medication, overwrite NFC tag)
- **FR-052**: System MUST validate all user input (positive numbers for doses/hours, valid time formats, non-empty required fields)
- **FR-053**: System MUST prevent accidental double-confirmation of same dose within short time window (e.g., 1 minute)
- **FR-054**: System MUST maintain data integrity during NFC write failures (no partial/corrupted data)
- **FR-055**: System MUST backup medication data to device storage to prevent loss on app reinstall

### Key Entities

- **Medication**: Represents a prescribed medication. Key attributes: unique ID, nickname (user-friendly name), medication information (notes/instructions), dose amount (text field), category (Life Dependent/Very Important/Beneficial), schedule type (interval-based or time-based), schedule details, end date (optional), refill threshold 1 (notification level), refill threshold 2 (alarm level), maximum dose count (for refill reset), current remaining doses, linked NFC tag IDs (multiple), active/inactive status, creation date.

- **Dose Record**: Represents a single dose event. Key attributes: medication reference, scheduled time, actual time (when taken/skipped), status (taken/skipped/missed), confirmation method (NFC scan or manual), remaining doses after this event, timestamp.

- **Schedule**: Represents timing configuration for a medication. Key attributes: medication reference, schedule type (interval or specific-times), interval in hours (if interval-based), specific times list (if time-based), end date (optional), next dose time (calculated), last dose time.

- **Alarm Configuration**: Represents alarm settings for a medication. Key attributes: medication reference, pre-dose alarm enabled, pre-dose minutes before, on-time alarm enabled, post-dose alarm enabled, post-dose minutes after, alarm sound file per category, volume level.

- **NFC Tag**: Represents an NFC tag linked to medication. Key attributes: unique tag ID (written to physical tag), medication reference (which medication it identifies), tag format (NDEF), creation/assignment date. Note: Tag itself only stores unique ID, not medication name.

- **Emergency Contact**: Represents a contact for missed dose notifications. Key attributes: contact name, email address (optional), phone number (optional), notification methods (email/SMS/call), trigger conditions (which categories, how many missed doses), associated medications (optional - if specific to certain meds).

- **Notification Settings**: Represents global notification preferences. Key attributes: emergency notifications enabled, default refill thresholds per category, default alarm configuration per category, manual confirmation mode enabled, GPS location sharing enabled, notification retry attempts.

- **App Settings**: Represents global application configuration. Key attributes: master volume, alarm sounds per category (Life Dependent/Very Important/Beneficial), NFC read/write timeout duration, dose confirmation timeout (how long before dose can be confirmed again), TTS (text-to-speech) voice settings, theme preferences.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can complete full medication setup (add medication, enter details, write NFC tag) in under 3 minutes on first attempt
- **SC-002**: NFC tag scans successfully read medication ID within 2 seconds in 95% of attempts
- **SC-003**: Audio confirmation speaks complete medication information (name, dose, next time) within 3 seconds of successful NFC scan
- **SC-004**: Scheduled alarms trigger within 30 seconds of configured time in 99% of cases
- **SC-005**: Users with cognitive issues can successfully take correct medication using only audio guidance without reading screen (measured through usability testing)
- **SC-006**: Application startup time is under 2 seconds on mid-range Android devices
- **SC-007**: Emergency notifications are sent within 5 minutes of missed critical medication dose
- **SC-008**: Zero data loss during application crashes or unexpected restarts (all medication data persists)
- **SC-009**: Users can view complete medication list and identify overdue medications within 5 seconds of opening app
- **SC-010**: Application uses less than 50MB of storage for typical user with 10 medications and 30 days of dose history
- **SC-011**: Refill reminders trigger before user runs out of medication in 95% of cases (based on tracking dose consumption)
- **SC-012**: Users successfully distinguish between medication categories by alarm sound alone in 90% of cases

## Assumptions

- Users have Android devices with NFC hardware capability (API Level 21 / Android 5.0 or higher)
- Users can physically attach NFC tags to medication bottles (tags are assumed to be provided separately)
- Users have basic familiarity with smartphone navigation (can tap buttons, navigate menus)
- Caregivers/family members may help with initial setup for users with severe cognitive impairments
- Emergency contacts (email, phone) are valid and monitored by responsible parties
- Users have granted necessary Android permissions: NFC, notifications, location (optional for emergency alerts), phone (for call notifications)
- Text-to-speech (TTS) engine is available on device (standard on Android 5.0+)
- Users keep sufficient phone battery charge to ensure alarms can trigger throughout day
- Medication schedules do not change frequently (app is designed for stable, ongoing prescriptions)
- Users understand the difference between "taken", "skipped", and "missed" dose statuses
- NFC tags are NDEF-formatted and writable (standard NFC tags, not read-only)
- Application will handle typical personal medication count (5-15 medications) efficiently
