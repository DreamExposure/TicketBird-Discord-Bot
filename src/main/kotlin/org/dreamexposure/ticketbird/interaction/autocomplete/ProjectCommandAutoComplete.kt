package org.dreamexposure.ticketbird.interaction.autocomplete

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.ProjectService
import org.springframework.stereotype.Component

@Component
class ProjectCommandAutoComplete(
    private val projectService: ProjectService,
) : AutoCompleteHandler {
    override val id = "project"

    override suspend fun handle(event: ChatInputAutoCompleteEvent) {
        when (event.focusedOption.name) {
            "name" -> projectName(event)
            else -> event.respondWithSuggestions(listOf()).awaitSingleOrNull()
        }
    }


    private suspend fun projectName(event: ChatInputAutoCompleteEvent) {
        val input = event.focusedOption.value
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get()

        val projects = projectService.getAllProjects(event.interaction.guildId.get())

        val toSend = projects
            // Check if prefix or name matches input
            .filter { it.name.contains(input, ignoreCase = true) || it.prefix.contains(input, ignoreCase = true) }
            // If nothing matches, default to showing all projects
            .ifEmpty { projects }
            .map {
                ApplicationCommandOptionChoiceData.builder()
                    .name(it.name)
                    .value(it.name)
                    .build()
            }

        event.respondWithSuggestions(toSend)
            .awaitSingleOrNull()
    }
}
