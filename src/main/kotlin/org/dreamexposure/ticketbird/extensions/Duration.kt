package org.dreamexposure.ticketbird.extensions

import java.time.Duration

fun Duration.getHumanReadable() =
    "%d d, %d h, %d m, %d s%n".format(toDays(), toHoursPart(), toMinutesPart(), toSecondsPart())

fun Duration.getHumanReadableMinimized(): String {
    val builder = StringBuilder()

    if (toDays() == 1L) builder.append("${toDays()} day")
    if (toDays() >= 2L) builder.append("${toDays()} days")

    if (toHoursPart() == 1) builder.append(", ${toHoursPart()} hour")
    if (toHoursPart() >= 2) builder.append(", ${toHoursPart()} hours")

    return builder.toString()
}
