---
stepsCompleted: [1, 2, 3]
inputDocuments: []
session_topic: 'KMP mobile app idea selection'
session_goals: 'Define a good, simple, buildable pet project using Kotlin Multiplatform'
selected_approach: 'pirate-code-brainstorm'
techniques_used: ['pirate-code-brainstorm', 'rapid-fire-ideation', 'stress-test-drill-down']
ideas_generated: ['habit tracker', 'water/caffeine intake logger', 'did I take my meds', 'trip packing checklist', 'gas mileage log', 'plant watering reminder', 'movie/book watchlist', 'expense splitter', 'pomodoro timer', 'local weather journal']
context_file: ''
---

# Brainstorming Session Results

**Facilitator:** Sonja
**Date:** 2026-04-02

## Session Overview

**Topic:** What KMP mobile app to build as a pet project
**Goals:** Find a simple, well-scoped use case that's a good fit for KMP, realistic to finish, and genuinely useful

### Session Setup

Rapid 10-minute session using Pirate Code Brainstorm — rapid-fire ideation followed by stress-test drill-down and a final decision.

## Technique Selection

**Approach:** Pirate Code Brainstorm (streamlined)
**Selection Method:** User-requested short path — 10 minutes to a decision

## Ideas Generated

10 rapid-fire KMP pet project ideas evaluated:

1. Habit tracker — daily streaks
2. Water/caffeine intake logger ⭐
3. "Did I take my meds?" yes/no daily log ⭐
4. Trip packing checklist
5. Gas mileage / fuel log
6. Plant watering reminder
7. Movie/book watchlist
8. Expense splitter
9. Pomodoro timer
10. Local weather journal

## Stress Test Results

**Option 2 — Water/Caffeine Intake Logger**
- Simple enough: Yes
- KMP value: High (multi-device, shared logic)
- Risk: Crowded space, scope creep temptation

**Option 3 — Supplements/Meds Tracker**
- Simple enough: Super simple — one screen, yes/no, timestamp, notification
- KMP value: Solid — notifications + local persistence = genuine KMP showcase
- Risk: Almost none — focused scope resists bloat

## Decision

**Selected Idea:** Supplements & Medications Daily Tracker

> A KMP mobile app that helps users track multiple daily supplements and medications — logging each one with a yes/no tap, sending reminders, and showing a simple history so nothing gets missed.

### Scope

| In | Out |
|---|---|
| Add/manage multiple meds/supplements | Dosage calculations |
| Daily yes/no log per item | Drug interaction warnings |
| Per-item reminder notifications | Syncing with health APIs |
| Simple history view (last 7 days) | Multi-user / caregiver mode |
| iOS + Android via KMP | Fancy charts |

### KMP Learning Value
- Shared data models + business logic (schedule, history)
- Local persistence (SQLDelight)
- Notifications (platform-specific, KMP-abstracted)
- Simple UI in Compose Multiplatform
