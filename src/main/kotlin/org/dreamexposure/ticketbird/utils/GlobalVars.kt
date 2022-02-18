package org.dreamexposure.ticketbird.utils

import discord4j.common.util.Snowflake
import discord4j.rest.util.Color
import org.slf4j.Marker
import org.slf4j.MarkerFactory

object GlobalVars {
    var iconUrl: String? = null
    val novaId = Snowflake.of(130510525770629121L)
    val embedColor = Color.of(252, 113, 20)
    val disallowed = arrayOf(
        "!", "@", "#", "$", "%", "^", "&", "*",
        "(", ")", "=", "+", "[", "]", "{", "}", "|", "\\", ";", ":", "'", "\"", ",", ".", "<", ">", "?", "/", "`", "~"
    )

    val DEFAULT: Marker = MarkerFactory.getMarker("DISCAL_WEBHOOK_DEFAULT")

    val STATUS: Marker = MarkerFactory.getMarker("DISCAL_WEBHOOK_STATUS")
}
