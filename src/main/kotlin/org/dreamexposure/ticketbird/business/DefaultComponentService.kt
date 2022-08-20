package org.dreamexposure.ticketbird.business

import discord4j.core.`object`.component.*
import discord4j.core.`object`.reaction.ReactionEmoji
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class DefaultComponentService(
    private val localeService: LocaleService,
    private val projectService: ProjectService,
) : ComponentService {
    override suspend fun getStaticMessageComponents(settings: GuildSettings): Array<LayoutComponent> {
        val button = Button.primary(
            "create-ticket",
            ReactionEmoji.unicode("\uD83D\uDCE8"), // Incoming envelop emote
            localeService.getString(settings.locale, "button.create-ticket.label")
        )

        return arrayOf(ActionRow.of(button))
    }

    override suspend fun getProjectSelectComponents(settings: GuildSettings): Array<LayoutComponent> {
        val projectsAsOptions = projectService.getAllProjects(settings.guildId)
            .map { SelectMenu.Option.of(it.name, it.name) }

        val selectMenu = SelectMenu.of("select-project", projectsAsOptions)
            .withPlaceholder(localeService.getString(settings.locale, "dropdown.select-project.placeholder"))

        return arrayOf(ActionRow.of(selectMenu))
    }

    override suspend fun getTicketOpenModalComponents(settings: GuildSettings): Array<LayoutComponent> {
        val infoInput = TextInput.paragraph(
            "ticket-detail.info",
            localeService.getString(settings.locale, "modal.ticket-detail.info.label"),
            localeService.getString(settings.locale, "modal.ticket-detail.info.placeholder")
        )

        return arrayOf(ActionRow.of(infoInput))
    }

    override suspend fun getTicketMessageComponents(settings: GuildSettings): Array<LayoutComponent> {
        val closeButton = Button.success(
            "close-ticket",
            ReactionEmoji.unicode("\u2714"), // Check mark emote
            localeService.getString(settings.locale, "button.close-ticket.label")
        )
        val holdButton = Button.secondary(
            "hold-ticket",
            ReactionEmoji.unicode("\u23F8"),
            localeService.getString(settings.locale, "button.hold-ticket.label")
        )


        return arrayOf(ActionRow.of(closeButton, holdButton))
    }
}
