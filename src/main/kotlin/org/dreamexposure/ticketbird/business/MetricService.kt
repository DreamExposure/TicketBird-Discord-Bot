package org.dreamexposure.ticketbird.business

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class MetricService(
    private val meterRegistry: MeterRegistry,
) {
    fun recordInteractionDuration(handler: String, type: String, duration: Long) {
        meterRegistry.timer(
            "bot.interaction.duration",
            listOf(Tag.of("handler", handler), Tag.of("type", type))
        ).record(Duration.ofMillis(duration))
    }

    fun recordTaskDuration(task: String, tags: List<Tag> = listOf(), duration: Long) {
        meterRegistry.timer(
            "bot.task.duration",
            tags.plus(Tag.of("task", task))
        ).record(Duration.ofMillis(duration))
    }

    fun recordTicketActivityTaskDuration(scope: String, duration: Long) {
        recordTaskDuration("ticket_activity", listOf(Tag.of("scope", scope)), duration)
    }

    fun recordTicketActionDuration(action: String, duration: Long) {
        meterRegistry.timer(
            "bot.ticketbird.ticket.action.duration",
            listOf(Tag.of("action", action))
        ).record(Duration.ofMillis(duration))
    }
}
