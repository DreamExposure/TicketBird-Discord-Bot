package org.dreamexposure.ticketbird.extensions.discord4j

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent
import java.time.Duration

fun DeferrableInteractionEvent.deleteFollowupDelayed(id: Snowflake, delay: Duration) = deleteFollowup(id).delaySubscription(delay)

fun DeferrableInteractionEvent.deleteReplyDelayed(delay: Duration) = deleteReply().delaySubscription(delay)
