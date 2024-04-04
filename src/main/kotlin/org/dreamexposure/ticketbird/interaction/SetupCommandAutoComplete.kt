package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.extensions.getHumanReadableMinimized
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class SetupCommandAutoComplete(
    private val localeService: LocaleService,
) : InteractionHandler<ChatInputAutoCompleteEvent> {
    override val ids = arrayOf("setup.action", "setup.message")

    override suspend fun handle(event: ChatInputAutoCompleteEvent, settings: GuildSettings) {
        when ("${event.commandName}.${event.focusedOption.name}") {
            "setup.action" -> actionTiming(event, settings)
            "setup.message" -> messageMessaging(event, settings)
            else -> event.respondWithSuggestions(listOf()).awaitSingleOrNull()
        }
    }

    private suspend fun actionTiming(event: ChatInputAutoCompleteEvent, settings: GuildSettings) {
        val input = event.focusedOption.value
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get()

        val actions = listOf(
            ApplicationCommandOptionChoiceData.builder()
                .name(localeService.getString(
                    settings.locale,
                    "auto-complete.setup.actions.auto-close",
                    settings.autoClose.getHumanReadableMinimized()
                )).value("auto-close")
                .build(),
            ApplicationCommandOptionChoiceData.builder()
                .name(localeService.getString(
                    settings.locale,
                    "auto-complete.setup.actions.auto-delete",
                    settings.autoDelete.getHumanReadableMinimized()
                )).value("auto-delete")
                .build()
        )
        val filtered = actions.filter { it.name().contains(input) || (it.value() as String).contains(input) }

        event.respondWithSuggestions(filtered.ifEmpty { actions })
            .awaitSingleOrNull()
    }

    private suspend fun messageMessaging(event: ChatInputAutoCompleteEvent, settings: GuildSettings) {
        val input = event.focusedOption.value
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get()

        val messages = listOf(
            ApplicationCommandOptionChoiceData.builder()
                .name(localeService.getString(settings.locale, "auto-complete.setup.messaging.support-message"))
                .value("support-message")
                .build(),
        )
        val filtered = messages.filter { it.name().contains(input) || (it.value() as String).contains(input) }

        event.respondWithSuggestions(filtered.ifEmpty { messages })
            .awaitSingleOrNull()
    }
}

