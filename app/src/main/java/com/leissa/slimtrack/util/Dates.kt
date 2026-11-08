package com.leissa.slimtrack.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Runs [onNewDay] shortly after every local midnight for as long as the scope lives.
 * Screens also refresh on resume, so a clock or time-zone change is picked up then.
 */
fun CoroutineScope.trackDayChanges(onNewDay: () -> Unit): Job = launch {
    while (isActive) {
        delay(millisUntilNextDay())
        onNewDay()
    }
}

/** Milliseconds from now until just after the next local midnight. */
private fun millisUntilNextDay(): Long {
    val now = ZonedDateTime.now()
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return (Duration.between(now, nextMidnight).toMillis() + 1_000L).coerceAtLeast(1_000L)
}
