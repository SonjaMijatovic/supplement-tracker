# Open Questions

## Weekend reminder behaviour when toggle is unchecked

**Question:** When "Different time on weekends?" is unchecked, does the item have a weekend reminder at the same time as the weekday reminder, or no weekend reminder at all?

**Context:** The current implementation stores `reminder_weekend_time = NULL` when the toggle is off, and passes `null` for `weekendTime` in `onSave`. Epic 4 (Notifications) will need to decide: does `NULL` mean "use weekday time" or "no reminder on weekends"?

**Where to resolve:** When implementing Epic 4 (notification scheduling engine, Story 4-2). Check `ItemEditSheet.kt` and the notification scheduling logic at that point.
