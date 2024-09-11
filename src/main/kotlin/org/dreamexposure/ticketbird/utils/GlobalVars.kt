package org.dreamexposure.ticketbird.utils

import discord4j.rest.util.Color
import org.slf4j.Marker
import org.slf4j.MarkerFactory

object GlobalVars {
    var iconUrl: String? = null
    val embedColor = Color.of(252, 113, 20)
    val successColor = Color.of(38, 255, 57)
    val errorColor: Color  = Color.of(248, 38, 48)
    val warnColor: Color = Color.of(232, 150, 0)

    val DEFAULT: Marker = MarkerFactory.getMarker("DISCAL_WEBHOOK_DEFAULT")

    val STATUS: Marker = MarkerFactory.getMarker("DISCAL_WEBHOOK_STATUS")
}
