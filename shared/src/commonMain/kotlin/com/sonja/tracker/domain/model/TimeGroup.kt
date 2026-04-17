package com.sonja.tracker.domain.model

data class TimeGroup(
    val timeSlot: String,           // "HH:mm" — matches reminder_*_time format
    val items: List<Item>,
    val allLogged: Boolean = false  // derived from log entries in story 3.2; always false in 3.1
)
