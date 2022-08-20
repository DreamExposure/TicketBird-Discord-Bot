package org.dreamexposure.ticketbird.business

import discord4j.core.`object`.component.LayoutComponent
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface ComponentService {
    suspend fun getStaticMessageComponents(settings: GuildSettings): Array<LayoutComponent>

    suspend fun getProjectSelectComponents(settings: GuildSettings): Array<LayoutComponent>

    suspend fun getTicketOpenModalComponents(settings: GuildSettings): Array<LayoutComponent>

    suspend fun getTicketMessageComponents(settings: GuildSettings): Array<LayoutComponent>
}
