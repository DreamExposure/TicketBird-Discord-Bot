package org.dreamexposure.ticketbird.extensions

import java.time.Duration

fun Duration.getHumanReadable() =
    "%d d, %d h, %d m, %d s%n".format(toDays(), toHoursPart(), toMinutesPart(), toSecondsPart())

fun Duration.getHumanReadableMinimized(): String {
    val builder = StringBuilder()

    if (toDays() > 0) builder.append("${toDays()} days")
    if (toHoursPart() > 0) builder.append(", ${toHoursPart()} hours")
    return builder.toString()
}
