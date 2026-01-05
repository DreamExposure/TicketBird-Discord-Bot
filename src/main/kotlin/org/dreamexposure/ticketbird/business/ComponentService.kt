package org.dreamexposure.ticketbird.business

import discord4j.core.`object`.component.*
import discord4j.core.`object`.emoji.Emoji
import org.dreamexposure.ticketbird.extensions.textInputPlaceholderSafe
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.Project
import org.springframework.stereotype.Component

@Component
class ComponentService(
    private val localeService: LocaleService,
    private val projectService: ProjectService,
) {
    suspend fun getStaticMessageComponents(settings: GuildSettings): Array<LayoutComponent> {
        val button = Button.primary(
            "create-ticket",
            Emoji.unicode("\uD83D\uDCE8"), // Incoming envelop emote
            localeService.getString(settings.locale, "button.create-ticket.label")
        ).disabled(settings.requiresRepair)

        return arrayOf(ActionRow.of(button))
    }

    suspend fun getProjectSelectComponents(settings: GuildSettings, withCreate: Boolean = false): Array<LayoutComponent> {
        val projectsAsOptions = projectService.getAllProjects(settings.guildId)
            .map { SelectMenu.Option.of(it.name, it.id.toString()) }

        val id = if (withCreate) "select-project-with-create" else "select-project"

        val selectMenu = SelectMenu.of(id, projectsAsOptions)
            .withPlaceholder(localeService.getString(settings.locale, "dropdown.select-project.placeholder"))

        return arrayOf(ActionRow.of(selectMenu))
    }

    suspend fun getTicketOpenModalComponents(settings: GuildSettings): Array<LayoutComponent> {
        val infoInput = TextInput.paragraph(
            "ticket-detail.info",
            0,
            4000,
        ).placeholder(localeService.getString(settings.locale, "modal.ticket-detail.info.placeholder"))
            .required(false)

        return arrayOf(Label.of(localeService.getString(settings.locale, "modal.ticket-detail.info.label"),infoInput))
    }

    suspend fun getTicketMessageComponents(settings: GuildSettings): Array<LayoutComponent> {
        val closeButton = Button.success(
            "close-ticket",
            Emoji.unicode("\u2714"), // Check mark emote
            localeService.getString(settings.locale, "button.close-ticket.label")
        )
        val holdButton = Button.secondary(
            "hold-ticket",
            Emoji.unicode("\u23F8"),
            localeService.getString(settings.locale, "button.hold-ticket.label")
        )

        return arrayOf(ActionRow.of(closeButton, holdButton))
    }

    suspend fun getEditSupportMessageModalComponents(settings: GuildSettings): Array<LayoutComponent> {
        val currentTitle = settings.staticMessageTitle ?: localeService.getString(settings.locale, "embed.static.title")
        val currentDesc = settings.staticMessageDescription ?: localeService.getString(settings.locale, "embed.static.desc")

        val titleInput = TextInput.small(
            "edit-support-message.title",
            0,
            255
        ).placeholder(localeService.getString(settings.locale, "modal.edit-support-message.title.placeholder").textInputPlaceholderSafe())
            .prefilled(currentTitle)
            .required(false)
        val descriptionInput = TextInput.paragraph(
            "edit-support-message.description",
            0,
            4000
        ).placeholder(localeService.getString(settings.locale, "modal.edit-support-message.description.placeholder").textInputPlaceholderSafe())
            .prefilled(currentDesc)
            .required(false)

        return arrayOf(
            Label.of(localeService.getString(settings.locale, "modal.edit-support-message.title.label"), titleInput),
            Label.of(localeService.getString(settings.locale, "modal.edit-support-message.description.label"), descriptionInput)
        )
    }

    suspend fun getAddProjectModalComponents(settings: GuildSettings): Array<LayoutComponent> {
        val nameInput = TextInput.small(
            "add-project.name",
            1,
            100
        ).placeholder(localeService.getString(settings.locale, "modal.add-project.name.placeholder"))
            .required()
        val prefixInput = TextInput.small(
            "add-project.prefix",
            1,
            16
        ).placeholder(localeService.getString(settings.locale, "modal.add-project.prefix.placeholder"))
            .required()
        val infoInput = TextInput.paragraph(
            "add-project.info",
            0,
            4000
        ).placeholder(localeService.getString(settings.locale, "modal.add-project.info.placeholder").textInputPlaceholderSafe())
            .required(false)

        return arrayOf(
            Label.of(localeService.getString(settings.locale, "modal.add-project.name.label"), nameInput),
            Label.of(localeService.getString(settings.locale, "modal.add-project.prefix.label"), prefixInput),
            Label.of(localeService.getString(settings.locale, "modal.add-project.info.label"), infoInput)
        )
    }

    suspend fun getEditProjectModalComponents(settings: GuildSettings, project: Project): Array<LayoutComponent> {
        val infoInput = TextInput.paragraph(
            "edit-project.info",
            0,
            4000
        ).placeholder(localeService.getString(settings.locale, "modal.edit-project.info.placeholder").textInputPlaceholderSafe())
            .prefilled(project.additionalInfo ?: "")
            .required(false)


        return arrayOf(Label.of(localeService.getString(settings.locale, "modal.edit-project.info.label"), infoInput))
    }
}
