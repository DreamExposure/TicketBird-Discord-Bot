package org.dreamexposure.ticketbird.extensions.discord4j

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingleOrNull
import java.time.Duration

@OptIn(DelicateCoroutinesApi::class)
fun DeferrableInteractionEvent.deleteFollowupDelayed(id: Snowflake, delay: Duration) = GlobalScope.launch {
    deleteFollowup(id).delaySubscription(delay).awaitSingleOrNull() }

@OptIn(DelicateCoroutinesApi::class)
fun DeferrableInteractionEvent.deleteReplyDelayed(delay: Duration)  = GlobalScope.launch {
    deleteReply().delaySubscription(delay).awaitSingleOrNull()
}
