package org.dreamexposure.ticketbird.extensions

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun Instant.ticketLogFileFormat(): String {
    return DateTimeFormatter.ofPattern("yyyy LLL dd HH:mm:ss z").withZone(ZoneOffset.UTC).format(this)
}
