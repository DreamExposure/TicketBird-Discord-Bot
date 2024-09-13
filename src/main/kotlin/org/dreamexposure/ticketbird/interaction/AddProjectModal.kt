package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent
import discord4j.core.`object`.component.TextInput
import discord4j.core.`object`.entity.Message
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.EmbedService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.ProjectService
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.Project
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class AddProjectModal(
    private val projectService: ProjectService,
    private val localeService: LocaleService,
    private val embedService: EmbedService,
): InteractionHandler<ModalSubmitInteractionEvent> {
    override val ids = arrayOf("add-project-modal")
    override val ephemeral = true

    private val messageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_GENERIC_SECONDS.getLong().asSeconds()

    override suspend fun handle(event: ModalSubmitInteractionEvent, settings: GuildSettings) {
        val inputs = event.getComponents(TextInput::class.java)

        val name = inputs.first { it.customId == "add-project.name" }.value.get()
        val prefix = inputs.first { it.customId == "add-project.prefix" }.value.get()
        val info = inputs.first { it.customId == "add-project.info" }.value.getOrNull()

        // Double check if max amount of projects have been created
        if (projectService.getAllProjects(settings.guildId).size >= 25) {
            event.createFollowup(localeService.getString(settings.locale, "command.project.add.limit-reached"))
                .withEmbeds(embedService.getProjectListEmbed(settings))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        val project = projectService.createProject(Project(
            guildId = settings.guildId,
            name = name,
            prefix = prefix,
            additionalInfo = info
        ))

        event.createFollowup(localeService.getString(settings.locale, "command.project.add.success"))
            .withEmbeds(embedService.getProjectViewEmbed(settings, project))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }
}
