package org.dreamexposure.ticketbird.extensions

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Instant.ticketLogFileFormat(): String {
    return DateTimeFormatter.ofPattern("yyyy LLL dd HH:mm:ss z").withZone(ZoneId.of("UTC")).format(this)
}

fun Instant.isExpiredTtl(): Boolean = Instant.now().isAfter(this)
