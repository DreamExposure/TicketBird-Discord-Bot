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
import org.dreamexposure.ticketbird.extensions.embedDescriptionSafe
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class EditProjectModal(
    private val projectService: ProjectService,
    private val localeService: LocaleService,
    private val embedService: EmbedService,
): InteractionHandler<ModalSubmitInteractionEvent> {
    override val ids = arrayOf("edit-project-modal")
    override val ephemeral = true

    private val messageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_GENERIC_SECONDS.getLong().asSeconds()

    override suspend fun handle(event: ModalSubmitInteractionEvent, settings: GuildSettings) {
        val guildId = event.interaction.guildId.get()
        val projectId = event.customId.split("-").last().toLong()
        val inputs = event.getComponents(TextInput::class.java)

        val info = inputs.first { it.customId == "edit-project.info" }.value.getOrNull()

        // update --- Short circuit if project doesn't exist
        val updatedProject = projectService.getProject(guildId, projectId)?.copy(
            additionalInfo = info?.embedDescriptionSafe()
        )
        if (updatedProject == null) {
            // Project doesn't exist,did they delete it before submitting the modal?
            event.createFollowup(localeService.getString(settings.locale, "command.project.edit.not-found"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }
        projectService.updateProject(updatedProject)

        // Return with project view embed
        event.createFollowup(localeService.getString(settings.locale, "command.project.edit.success"))
            .withEmbeds(embedService.getProjectViewEmbed(settings, updatedProject))
            .withEphemeral(ephemeral)
            .awaitSingleOrNull()
    }

}