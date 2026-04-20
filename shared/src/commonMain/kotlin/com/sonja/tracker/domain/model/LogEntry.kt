package com.sonja.tracker.domain.model

data class LogEntry(
    val id: Long,
    val itemId: Long,
    val date: String,       // "YYYY-MM-DD"
    val state: LogState,
    val loggedAt: String    // ISO datetime string
)
