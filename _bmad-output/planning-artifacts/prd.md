---
stepsCompleted: ["step-01-init", "step-02-discovery", "step-02b-vision", "step-02c-executive-summary", "step-03-success", "step-04-journeys", "step-05-domain", "step-06-innovation", "step-07-project-type", "step-08-scoping", "step-09-functional", "step-10-nonfunctional", "step-11-polish"]
classification:
  projectType: mobile_app
  domain: general
  complexity: low
  projectContext: greenfield
inputDocuments:
  - "_bmad-output/brainstorming/brainstorming-session-2026-04-02-001.md"
  - "_bmad-output/planning-artifacts/product-brief-Projects.md"
workflowType: 'prd'
---

# Product Requirements Document — Daily Supplement & Medication Tracker

**Author:** Sonja
**Date:** 2026-04-02

## Executive Summary

A Kotlin Multiplatform mobile app for iOS and Android that solves one problem cleanly: helping health-conscious adults reliably take their daily supplements and medications. Target users manage 3–6 items at different times of day and need per-item reminders and a frictionless log — not medical software. The app delivers a single-tap yes/no logging experience, grouped timed notifications, and a rolling 7-day history. All data is local; no account, no cloud, no complexity.

Primary audience: two users in a shared household, each with an independent daily routine of one medication and 5–6 supplements. Built by the developer for personal daily use, with an eye toward App Store / Play Store publication.

### What Makes This Special

The supplement tracker market is dominated by full-featured medication management apps (Medisafe, MyTherapy, RoundHealth) designed for complex prescription regimens. For users with a simple supplement routine, these tools are overkill — cluttered, paternalistic, and often paywalled. This app's differentiator is deliberate restraint: a narrow, stable feature set that resists scope creep by design.

Core insight: daily supplement-takers need a habit tracker, not a medical tool. Per-item morning/evening reminders address the actual usage pattern. Zero onboarding friction (no account, no health data sync) removes every barrier to adoption. Privacy by default — data never leaves the device — is a genuine advantage in a category where competitors monetize health data.

Secondary differentiator: built with Compose Multiplatform + SQLDelight, the app is a clean KMP reference implementation — shared business logic, local persistence, and platform-abstracted notifications across iOS and Android from a single codebase.

## Project Classification

- **Project Type:** Mobile app (iOS + Android via Kotlin Multiplatform / Compose Multiplatform)
- **Domain:** General — wellness/habit tracking (health-adjacent; explicitly outside regulated healthcare)
- **Complexity:** Low — tight scope, no compliance requirements, no external integrations, no backend
- **Project Context:** Greenfield

## Success Criteria

### User Success

- Both primary users log their daily routine for 30+ consecutive days without reverting to memory or other tracking methods
- Full day's routine logged in under 30 seconds across all items
- Per-item reminders arrive at the correct scheduled time; user never opens the app wondering "did I already take this?"
- App is functional from install with no account, no onboarding form, no mandatory setup

### Business Success

- App in daily active use by both users within 7 days of first install
- Notification delivery rate ≥ 95% on both iOS and Android
- No data loss across normal device usage (restart, app update, OS update)

### Technical Success

- KMP architecture demonstrates clean separation of shared logic (data models, schedule management, history) from platform-specific concerns (notifications, UI); reviewable as a CV-worthy codebase
- One-page document explaining why and how the project was built — suitable as a portfolio companion
- Codebase passes self-review: no shortcuts, no hacks, no embarrassing workarounds
- App Store / Play Store publication-ready structure (bundle IDs, signing, permissions declarations) — not required for v1, but no structural blockers if pursued later

### Measurable Outcomes

| Outcome | Target |
|---|---|
| Daily active use | Both users, within 7 days of install |
| Notification delivery | ≥ 95% on iOS and Android |
| Logging speed | Full routine < 30 seconds |
| Data integrity | Zero loss across normal device lifecycle |
| Code quality | Shareable in a CV / portfolio without modification |

## User Journeys

### Journey 1: First Setup (Sonja, Day 1)

Sonja installs the app. No login. She adds her six items — name and reminder time each. Four morning items at 8:00 AM weekdays / 9:00 AM weekends, ashwagandha at 9:00 PM, medication at 10:00 PM. Takes five minutes.

The first screen shows all six items for today. Nothing is pending yet — it's 7:30 AM. At 8:00 AM a grouped notification arrives: *"Time for your morning routine — 4 items."* She taps **YES** directly on the notification. All four are logged without opening the app.

That evening she gets a notification for ashwagandha alone. She taps YES. Done. Medication at 10:00 PM — same. She opens the app before bed: all six items show as done with a friendly "All done today" message and icon.

**Capabilities revealed:** Add items, first screen shows all today's items, grouped notification with YES bulk-action, notification suppressed if item already logged, "all done" empty state.

---

### Journey 2: Daily Use — Missed Notification (Partner, Week 2)

Thursday morning, phone on silent, missed the notification. At 11:00 AM he opens the app. First screen shows morning items still pending. He taps each one manually — same interaction as a notification-triggered open.

That evening he realizes he missed logging yesterday's evening medication. He opens the history, navigates to yesterday, and logs it there. The app allows logging up to one day back only.

**Capabilities revealed:** Manual in-app logging, late logging (1 day back), 7-day history with empty slots for days before first use, any tap = yes regardless of time.

---

### Journey 3: Routine Change + Edit (Sonja, Month 2)

New medication, different schedule. Sonja taps edit on the old medication, updates name and time in place. She also adds a new supplement. One morning she manually logs items early — the notification fires later and is suppressed. She accidentally double-taps an item. No undo in v1 — accepted limitation.

**Capabilities revealed:** Edit item in place, add items anytime, notification suppressed for already-logged items, read-only log (accepted MVP limitation).

---

### Journey Requirements Traceability

| Capability | Journey | FR |
|---|---|---|
| Add / edit (in place) / delete items | 1, 3 | FR1–FR5 |
| Per-item scheduling with weekday/weekend split | 1, 2 | FR1, FR2 |
| Grouped notification per shared time slot | 1 | FR24 |
| Notification YES action — bulk-logs all items in group | 1 | FR26 |
| Notification suppressed if items already logged | 3 | FR27 |
| First screen: all today's items (pending + done) | 1, 2 | FR20, FR21 |
| "All done" empty state with icon | 1 | FR22 |
| Single-tap in-app logging (same UX regardless of entry point) | 1, 2 | FR14, FR16 |
| Late logging: up to 1 day back | 2 | FR17 |
| 7-day rolling history | 2 | FR35–FR38 |
| Timezone: v1 limitation — device local time only | All | NFR/known limitation |
| Read-only log, no undo | 3 | FR39 |

## Mobile App Specific Requirements

### Platform Requirements

| Platform | Minimum Version | Notes |
|---|---|---|
| Android | API 26 (Android 8.0) | Required for notification channels; exact alarms require API 31+ permission |
| iOS | iOS 16+ | Modern SwiftUI-era baseline |
| Shared (KMP) | Kotlin 1.9+ / KMP stable | Compose Multiplatform for UI |

No web or desktop targets. Single codebase for all shared logic; platform-specific implementations for notifications and native time picker only.

### Offline Mode

Fully offline — no network dependency at runtime. All data stored locally via SQLDelight. No sync, no cloud backup.

### Notification Strategy

- **Type:** Local notifications only — no remote push, no APNs backend, no FCM
- **Scheduling:** Per-item, time-based; grouped when multiple items share the same reminder time slot
- **Weekday/weekend split:** Each item stores two reminder times (`reminder_weekday_time`, `reminder_weekend_time`); scheduler selects based on current day
- **Notification action:** Single "YES" action button — bulk-logs all items in the group without opening the app; notification payload must carry item IDs or time-slot key
- **Suppression:** Pending notification cancelled by ID if all items in the group are already logged; requires `cancelNotification(id)` on the notification interface
- **Stale payload prevention:** Any item add, edit, or delete immediately reschedules all notifications for the affected time slot
- **Scheduling window:** 7-day lookahead (grouped time slots stay well within iOS 64-notification cap); rescheduled on every app foreground and on any item change
- **Notification identifier scheme:** `{itemId}_{date}_{timeSlot}` — consistent across platforms for targeted cancellation
- **Permission UX:** Requested on first launch; if denied, in-app prompt banner displayed; manual logging remains fully functional without permissions
- **Android timing:** `setExactAndAllowWhileIdle` / `RTC_WAKEUP`; Doze mode may delay reminders 5–15 minutes on aggressive battery savers — accepted known limitation
- **Lapse recovery:** If scheduling window lapses (app not opened for >7 days), notifications reschedule on next foreground with subtle "Reminders rescheduled" confirmation

### Notification Interface (expect/actual)

The KMP notification abstraction must expose at minimum:
- `scheduleNotification(id, itemIds, timeSlot, title)` — schedule a grouped notification
- `cancelNotification(id)` — cancel a pending notification by identifier
- `cancelAllNotifications()` — used on item delete and app reset

Suppression logic (already-logged check) lives in `:shared` — platform implementations are thin schedulers only. No local-scheduling concerns (alarm IDs, OS-specific timing) leak into `:shared`; `:shared` expresses only *what* to notify and *when*. This keeps FCM-backed delivery viable for v2 without rewriting shared logic.

### Device Permissions

| Permission | Platform | Purpose | Behaviour if Denied |
|---|---|---|---|
| POST_NOTIFICATIONS | Android 13+ | Local reminder delivery | In-app prompt; manual logging unaffected |
| SCHEDULE_EXACT_ALARM | Android 12+ | Precise reminder timing | Fallback to inexact alarms |
| UserNotifications | iOS | Local reminder delivery | In-app prompt; manual logging unaffected |
| Camera | iOS + Android | Item image capture | Camera option hidden; gallery option unaffected |
| Photo Library / Media | iOS + Android | Item image selection | Gallery option hidden; camera option unaffected |
| Both camera + photo library denied | Both | — | Image placeholder hidden throughout app |

### Log Entry States

| State | Meaning |
|---|---|
| `logged` | Item tapped as done on the correct day |
| `late-logged` | Item logged the following day (1-day-back) |
| `not-logged` | Day has passed; item was not logged |
| `no-data` | Day is before app install or item creation |

Days before app installation are not shown in history. Days between install and item creation show `no-data`.

### Data & Schema

- SQLDelight schema in `:shared` — single source of truth
- Weekday/weekend reminders: two nullable time fields per item (`reminder_weekday_time`, `reminder_weekend_time`), not a join table
- All schema changes ship with an explicit migration script; migration failure = hard crash with clear error, never silent data loss
- Database stored in backup-eligible directory: iOS `Application Support/`, Android Auto Backup eligible — supports OS-level device-to-device migration on both platforms

### Store Compliance

- Publication is a stretch goal; v1 structure must be publication-ready (bundle IDs, signing, iOS privacy manifest, permissions declarations)
- Category: Health & Fitness — supplement tracker; no diagnostic or therapeutic claims
- Single-device, single-user design; shared-phone use explicitly unsupported in v1

### Implementation Considerations

- KMP module structure: `:shared` (business logic, schema, scheduling), `:androidApp`, `:iosApp`
- Native OS time picker via `expect`/`actual` — iOS `UIDatePicker` (wheel style), Android Material3 clock dial
- YES notification action writes to SQLDelight even when app is fully force-quit: requires **Notification Service Extension** (iOS) and **BroadcastReceiver** (Android), each capable of initialising SQLDelight independently
- Reschedule triggered by: app foreground, item add, item edit, item delete
- No background sync or background fetch

## Project Scoping & Phased Development

### MVP Strategy

**Approach:** Problem-solving MVP — minimum to solve a real daily problem for two real users. No market validation needed.

**Done when:** Both users complete their first full week of daily logging.

**Resources:** Solo developer (Sonja), learning KMP/Compose Multiplatform with Claude Code. No backend, no team, no external dependencies.

### Phase 1 — MVP

- Add / edit (in place) / delete supplements and medications
- Per-item reminder scheduling with weekday/weekend split via native OS time picker
- Item image (gallery or camera) displayed as icon next to item name throughout app
- Grouped local notifications per time slot with YES bulk-action
- Notification suppression, stale-payload rescheduling, 7-day lookahead
- First screen: all today's items (pending + done); empty states for no-items and all-done
- Single-tap in-app logging regardless of entry point
- Late logging: up to 1 day back
- 7-day rolling history with 4 log states
- Local persistence via SQLDelight with migration support
- iOS + Android via Compose Multiplatform
- Notification permission graceful degradation with in-app banner

### Phase 2 — Post-MVP

- Onboarding flow explaining app usage
- Additional features only if genuine daily-use need emerges

### Phase 3 — Stretch

- App Store and Play Store publication — free, no upsell
- Lightweight multi-profile support (two routines on one device)
- Open-source release as a documented KMP reference implementation

### Risk Mitigation

| Risk | Severity | Mitigation |
|---|---|---|
| Notification implementation (expect/actual, NSE/BroadcastReceiver, stale-payload rescheduling) | High | Build and validate notifications first; ship without YES action if too complex (fallback: reminder-only) |
| SQLDelight migrations | Medium | Define schema fully before writing queries; perform dummy migration during development |
| Compose Multiplatform maturity | Low | Stable for this use case; no cutting-edge APIs required |

**Core experience framing:** App delivers full value through manual daily opens. Notifications are a convenience layer — up to 40% of iOS users decline notification permission on first prompt. The home screen must feel complete without any notifications.

## Functional Requirements

### Item Management

- **FR1:** User can add a supplement or medication with a name and weekday reminder time via the native OS time picker
- **FR2:** User can set a separate weekend reminder time per item via the native OS time picker
- **FR3:** User can edit an existing item's name in place
- **FR4:** User can edit an existing item's weekday and weekend reminder times via the native OS time picker
- **FR5:** User can delete an existing item
- **FR6:** User can view all configured items in a list
- **FR7:** User can attach an image to an item by selecting from the device gallery or taking a photo
- **FR8:** An image placeholder is displayed next to the item name throughout the app (main screen, history, item list) wherever the item name appears, except in notifications
- **FR9:** If the user has attached an image, it is shown in the placeholder; otherwise a default icon is displayed
- **FR10:** If the user has permanently declined both camera and photo library permissions, no image placeholder is shown anywhere in the app
- **FR11:** System requests camera permission when user first attempts to take a photo for an item image
- **FR12:** System requests photo library / media access permission when user first attempts to select an image from the gallery
- **FR13:** If one permission is denied, the corresponding option is unavailable; the other remains available if its permission is granted

### Daily Logging

- **FR14:** User can log an item as taken with a single tap from the main screen
- **FR15:** User can log all items in a notification group as taken via the YES bulk-action without opening the app
- **FR16:** User can log items by opening the app directly, with the same interaction as a notification-triggered open
- **FR17:** User can log items from the previous day (up to 1 day back)
- **FR18:** System records any log entry as "taken" for the day regardless of the time it was logged

### Main Screen & Today View

- **FR19:** User sees an empty state prompting item creation when no items are configured
- **FR20:** User can see all configured items for today (both pending and already logged) on the main screen
- **FR21:** User can visually distinguish pending items from already-logged items on the main screen
- **FR22:** User sees an "all done" confirmation state with message and icon when all today's items are logged
- **FR23:** User sees an in-app prompt to enable notifications if notification permission has been denied

### Notification Delivery

- **FR24:** System sends a single grouped notification for all items sharing the same reminder time slot
- **FR25:** System sends a separate notification for items with a unique reminder time
- **FR26:** Notification includes a YES bulk-action that logs all items in the group as taken
- **FR27:** System suppresses a scheduled notification if all items in the group are already logged for the day
- **FR28:** System reschedules affected time-slot notifications immediately when any item is added, edited, or deleted
- **FR29:** System reschedules all pending notifications when the app is foregrounded
- **FR30:** System maintains a 7-day notification lookahead window
- **FR31:** System detects scheduling lapse and displays a subtle "Reminders rescheduled" confirmation on next app open
- **FR32:** System requests notification permission on first launch
- **FR33:** App remains fully functional for manual logging when notification permission is denied
- **FR34:** System writes a log entry when the YES notification action is triggered, even when the app is fully force-quit

### History & Log Review

- **FR35:** User can view a rolling 7-day history for each item
- **FR36:** Each day in the history displays one of four states per item: logged, late-logged, not-logged, no-data
- **FR37:** History displays only days from app installation onwards; days before install are not shown; history shows fewer than 7 days if app was installed less than 7 days ago
- **FR38:** History displays no-data state for days between app installation and item creation date
- **FR39:** History is read-only; no editing or deletion of past log entries

### Data & Persistence

- **FR40:** App stores all data locally with no network requirement at any time
- **FR41:** App data persists across app restarts, OS updates, and normal device lifecycle
- **FR42:** App data stored in backup-eligible location on both iOS (`Application Support/`) and Android (Auto Backup), supporting OS-level device-to-device migration
- **FR43:** App applies database schema migrations on update without data loss; migration failure surfaces a clear error

## Non-Functional Requirements

### Performance

- App launch to interactive state: < 2 seconds on mid-range devices (Android API 26 equivalent, iPhone XR equivalent)
- Single-tap log action to visual confirmation: < 500ms
- Notification rescheduling on app foreground: non-blocking, completes in background within 1 second
- History view load (7-day data): < 1 second

### Privacy

- All user data stored exclusively on-device; no data transmitted to any external server
- No analytics, crash reporting, or telemetry that transmits personal or usage data
- Item images stored in app-private storage, not accessible to other apps or the system media gallery
- No advertising SDKs or third-party tracking libraries

### Reliability

- App must not crash during normal use; errors logged locally, never surfaced as unhandled exceptions
- All database writes atomic — no partial writes on unexpected termination
- Data persists correctly across: app force-quit, app update, OS update, device restart
- Notification delivery rate ≥ 95% under normal device conditions (charged device, network not required)

### Accessibility

- All interactive elements meet minimum touch target size: 44×44pt (iOS) / 48×48dp (Android)
- App supports Dynamic Type / system font scaling on both platforms without layout breakage
- No functionality or state conveyed by colour alone; pending vs. logged states use shape or label in addition to colour
- Core flows navigable with VoiceOver (iOS) and TalkBack (Android)
