package org.dreamexposure.ticketbird.extensions

import discord4j.common.util.Snowflake

fun Long.toSnowflake() = Snowflake.of(this)
