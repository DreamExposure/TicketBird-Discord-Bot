package org.dreamexposure.ticketbird.database

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono

interface GuildSettingsRepository: R2dbcRepository<GuildSettingsData, Long> {

    @Query("""
        UPDATE guild_settings
        SET lang = :lang,
            patron_guild = :patron_guild,
            dev_guild = :dev_guild,
            use_projects = :useProjects,
            awaiting_category = :awaitingCategory,
            responded_category = :respondedCategory,
            hold_category = :holdCategory,
            close_category = :closeCategory,
            support_channel = :supportChannel,
            static_message = :staticMessage,
            next_id = :nextId,
            staff = :staff
        WHERE guild_id = :guildId
    """)
    fun updateById(
        guildId: Long,
        lang: String,
        patronGuild: Boolean,
        devGuild: Boolean,
        useProjects: Boolean,

        awaitingCategory: Long?,
        respondedCategory: Long?,
        holdCategory: Long?,
        closeCategory: Long?,
        supportChannel: Long?,
        staticMessage: Long?,

        nextId: Int,
        staff: String,
    ): Mono<Int>
}
