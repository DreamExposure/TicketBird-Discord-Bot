package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent
import discord4j.core.`object`.component.TextInput
import discord4j.core.`object`.entity.Message
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.StaticMessageService
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class EditSupportMessageModal(
    private val settingsService: GuildSettingsService,
    private val staticMessageService: StaticMessageService,
    private val localeService: LocaleService,
): InteractionHandler<ModalSubmitInteractionEvent> {
    override val ids = arrayOf("edit-support-message-modal")
    override val ephemeral = true

    private val messageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_GENERIC_SECONDS.getLong().asSeconds()

    override suspend fun handle(event: ModalSubmitInteractionEvent, settings: GuildSettings) {
        val inputs = event.getComponents(TextInput::class.java)

        val title = inputs.first { it.customId == "edit-support-message.title" }.value.getOrNull()
        val description = inputs.first { it.customId == "edit-support-message.description" }.value.getOrNull()

        val newSettings = settings.copy(
            staticMessageTitle = if (!title.isNullOrBlank()) title else null,
            staticMessageDescription = if (!description.isNullOrBlank()) description else null
        )
        settingsService.updateGuildSettings(newSettings)

        val message = staticMessageService.update(settings.guildId) ?: throw IllegalStateException("Could not complete interaction as message is null")

        val followupContent = localeService.getString(
            settings.locale,
            "command.setup.messaging.edit-support-message.success",
            "https://discord.com/channels/${settings.guildId.asLong()}/${message.channelId.asLong()}/${message.id.asLong()}"
        )

        event.createFollowup(followupContent)
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }
}
