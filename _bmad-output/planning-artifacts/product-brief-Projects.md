---
title: "Product Brief: Daily Supplement & Medication Tracker"
status: "draft"
created: "2026-04-02"
updated: "2026-04-02"
inputs: ["_bmad-output/brainstorming/brainstorming-session-2026-04-02-001.md"]
---

# Product Brief: Daily Supplement & Medication Tracker

## Executive Summary

Most people who take daily supplements and medications aren't managing a medical regimen — they're managing a habit. Yet every app built to help them is designed for patients: dense with drug interaction warnings, dosage calculators, health API integrations, and subscription paywalls. For someone who takes a morning handful of vitamins and an evening medication, this is all noise.

This app takes a different approach: a clean, focused Kotlin Multiplatform mobile app for iOS and Android that does exactly one thing well. You add your supplements and medications, set a reminder time per item, and each day you tap yes or no. That's it. A 7-day history shows you how you're doing. No accounts, no health data sync, no complexity.

Built with Compose Multiplatform, SQLDelight, and platform-native notifications, the app is also a working showcase of what KMP can produce: a polished, genuinely useful tool with shared business logic across both platforms — and a concrete learning artifact for the developer building it.

## The Problem

People taking multiple daily supplements — a routine for millions of health-conscious adults — have no good tool for the job. The problem has two sides.

**Missing doses without realizing it.** When you take 5–6 items at different times of day, memory is unreliable. Did I take my magnesium this morning or just think about it? Was it yesterday? A week later, there's no record.

**Every existing app is overkill.** Tools like Medisafe, MyTherapy, and RoundHealth are built for complex medication management: multiple prescriptions, caregiver sharing, drug interaction checks, doctor integrations. For someone who wants to track their fish oil and vitamin D, the experience is overwhelming, paternalistic, and often paywalled. Users either over-engineer their solution or give up tracking entirely.

The gap: a simple, clean daily logger with targeted reminders that treats supplements as a habit to track — not a medical problem to solve.

## The Solution

A minimal, well-designed mobile app — iOS and Android — that puts supplement and medication tracking at zero friction.

**Core experience:**
- Add each supplement or medication with a name and a reminder time (morning or evening)
- Receive a clean, per-item notification at the right time each day
- Tap once to mark it done or skipped — no form, no confirmation flow
- See a simple 7-day history: did you take it, or not

The UI is calm and uncluttered. No dashboard, no streak rings, no gamification. Just a clear answer to: *"Did I take everything today?"*

## What Makes This Different

**Radical simplicity as a deliberate product decision.** Competing apps treat complexity as a feature. This app treats simplicity as the product. The scope is intentionally narrow and will stay there.

**Per-item reminders, not a daily blast.** A single daily notification fails users who take things at different times. This app notifies you for each item, when that item is due — morning and evening, separately.

**No account. No cloud. No friction.** All data is local. No signup, no backend to trust, no subscription wall. The app opens and works. The trade-off — no cross-device sync, no recovery if the device is wiped — is accepted intentionally.

**KMP without compromise.** Built with Compose Multiplatform + SQLDelight, this isn't a web wrapper — it's a genuinely native-feeling cross-platform experience with fully shared business logic and local persistence, deployable to both iOS and Android from a single codebase. Notification scheduling is platform-specific under a shared KMP abstraction — one of the more instructive parts of the build.

**Private by design.** In a category where most apps monetize health data, this app stores nothing outside your device. No account, no telemetry, no server.

## Who This Serves

**Primary users:** Health-conscious adults managing a daily routine of 3–6 supplements alongside one or more medications. They're consistent and health-aware but not medical professionals. They want to build a reliable habit, not track a health record.

**Specifically:** A two-person household, each with their own independent routine (one medication + 5–6 supplements daily). The app is designed for single-user, per-device use — your phone, your routine.

**The aha moment:** The first morning a reminder arrives for the right item at the right time, you tap it, and the log updates without any further interaction. No app is this frictionless for this exact problem.

## Success Criteria

The app succeeds when:

- Both primary users complete their daily routine in the app for 30+ consecutive days without reverting to memory or other methods
- Per-item notifications fire reliably on both iOS and Android
- Logging a full day's routine takes under 30 seconds total across all items
- The KMP architecture demonstrates clean separation of shared logic (data models, schedule management, history) from platform-specific concerns (notifications, UI polish) — a portfolio-ready reference implementation
- The app is publication-ready for App Store and Play Store with no structural blockers

## Scope

**Version 1 — In:**
- Add, edit, and delete supplements / medications (name + reminder time)
- Per-item daily notifications (morning and evening)
- Single-tap yes/no logging per item per day
- 7-day history view per item
- Local data persistence via SQLDelight
- iOS + Android via Compose Multiplatform

**Explicitly out of V1:**
- Dosage tracking or calculations
- Drug interaction warnings
- Health API sync (Apple Health, Google Fit)
- Multi-user or family profiles
- Cloud backup or account system
- Streak tracking, charts, or gamification
- Caregiver or sharing mode

## Roadmap Thinking

If the app becomes a genuine daily habit for its first two users, natural expansions include:

- **Lightweight multi-profile support** — two routines on one device, clearly separated
- **Simple weekly summary** — a plain-text export for doctor visits or personal review
- **Open-source reference release** — as a documented KMP best-practices showcase for the community
- **App Store / Play Store publication** — with a "free forever, no upsell" positioning that leans into the anti-complexity story
