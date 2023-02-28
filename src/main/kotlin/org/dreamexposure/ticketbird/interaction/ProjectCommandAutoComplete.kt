package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.ProjectService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class ProjectCommandAutoComplete(
    private val projectService: ProjectService,
) : InteractionHandler<ChatInputAutoCompleteEvent> {
    override val ids = arrayOf("project.project", "support.topic", "topic.topic")

    override suspend fun handle(event: ChatInputAutoCompleteEvent, settings: GuildSettings) {
        when ("${event.commandName}.${event.focusedOption.name}") {
            "project.project" -> projectName(event)
            "support.topic",
            "topic.topic" -> checkUsingFirst(event, settings)
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
                    .value(it.id.toString())
                    .build()
            }

        event.respondWithSuggestions(toSend)
            .awaitSingleOrNull()
    }

    private suspend fun checkUsingFirst(event: ChatInputAutoCompleteEvent, settings: GuildSettings) {
        // If not using projects, no need to waste their time
        if (settings.useProjects) return projectName(event)
        else event.respondWithSuggestions(listOf()).awaitSingleOrNull()
    }
}
