package org.dreamexposure.ticketbird.logger

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.WebhookEmbed
import club.minnced.discord.webhook.send.WebhookEmbedBuilder
import org.dreamexposure.ticketbird.GitProperty
import org.dreamexposure.ticketbird.TicketBird
import org.dreamexposure.ticketbird.conf.BotSettings
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.dreamexposure.ticketbird.utils.GlobalVars.DEFAULT
import org.dreamexposure.ticketbird.utils.GlobalVars.STATUS

class DiscordWebhookAppender : AppenderBase<ILoggingEvent>() {
    private val defaultHook: WebhookClient?
    private val statusHook: WebhookClient?

    init {
        if (BotSettings.USE_WEBHOOKS.get().equals("true", true)) {
            defaultHook = WebhookClient.withUrl(BotSettings.DEBUG_WEBHOOK.get())
            statusHook = WebhookClient.withUrl(BotSettings.STATUS_WEBHOOK.get())
        } else {
            defaultHook = null
            statusHook = null
        }
    }

    override fun append(eventObject: ILoggingEvent) {
        if (BotSettings.USE_WEBHOOKS.get().equals("true", true)) {
            when {
                eventObject.marker.equals(DEFAULT) -> executeDefault(eventObject)
                eventObject.marker.equals(STATUS) -> executeStatus(eventObject)
            }
        }
    }

    private fun executeStatus(event: ILoggingEvent) {
        val content = WebhookEmbedBuilder()
            .setTitle(WebhookEmbed.EmbedTitle("Status", null))
            .addField(WebhookEmbed.EmbedField(true, "Shard Index", "${TicketBird.getShardIndex()}"))
            .addField(WebhookEmbed.EmbedField(true, "Time", "<t:${event.timeStamp / 1000}:f>"))
            .addField(WebhookEmbed.EmbedField(false, "Logger", event.loggerName))
            .addField(WebhookEmbed.EmbedField(true, "Level", event.level.levelStr))
            .addField(WebhookEmbed.EmbedField(true, "Thread", event.threadName))
            .setDescription(event.formattedMessage)
            .setColor(GlobalVars.embedColor.rgb)
            .setFooter(WebhookEmbed.EmbedFooter("v${GitProperty.TICKETBIRD_VERSION.value}", null))

        if (event.throwableProxy != null) {
            content.addField(WebhookEmbed.EmbedField(false, "Error Message", event.throwableProxy.message))
            content.addField(
                WebhookEmbed.EmbedField(
                    false,
                    "Stacktrace",
                    "Stacktrace can be found in exceptions log file"
                )
            )
        }

        this.statusHook?.send(content.build())
    }

    private fun executeDefault(event: ILoggingEvent) {
        val content = WebhookEmbedBuilder()
            .setTitle(WebhookEmbed.EmbedTitle(event.level.levelStr, null))
            .addField(WebhookEmbed.EmbedField(true, "Shard Index", "${TicketBird.getShardIndex()}"))
            .addField(WebhookEmbed.EmbedField(true, "Time", "<t:${event.timeStamp / 1000}:f>"))
            .addField(WebhookEmbed.EmbedField(false, "Logger", event.loggerName))
            .addField(WebhookEmbed.EmbedField(true, "Level", event.level.levelStr))
            .addField(WebhookEmbed.EmbedField(true, "Thread", event.threadName))
            .setDescription(event.formattedMessage)
            .setColor(GlobalVars.embedColor.rgb)
            .setFooter(WebhookEmbed.EmbedFooter("v${GitProperty.TICKETBIRD_VERSION.value}", null))

        if (event.throwableProxy != null) {
            content.addField(WebhookEmbed.EmbedField(false, "Error Message", event.throwableProxy.message))
            content.addField(
                WebhookEmbed.EmbedField(
                    false,
                    "Stacktrace",
                    "Stacktrace can be found in exceptions log file"
                )
            )
        }

        this.defaultHook?.send(content.build())
    }


}
