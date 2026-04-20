package com.sonja.tracker.domain.model

data class TimeGroup(
    val timeSlot: String,                       // "HH:mm" — matches reminder_*_time format
    val items: List<Item>,
    val allLogged: Boolean = false,             // true when every item in this group is logged
    val loggedItemIds: Set<Long> = emptySet()  // IDs of logged items in this group (story 3.2+)
)
