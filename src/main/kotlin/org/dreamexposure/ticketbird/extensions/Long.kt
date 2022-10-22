package org.dreamexposure.ticketbird.extensions

import discord4j.common.util.Snowflake
import java.time.Duration

fun Long.toSnowflake() = Snowflake.of(this)

fun Long.asSeconds(): Duration = Duration.ofSeconds(this)

fun Long.asMinutes(): Duration = Duration.ofMinutes(this)
