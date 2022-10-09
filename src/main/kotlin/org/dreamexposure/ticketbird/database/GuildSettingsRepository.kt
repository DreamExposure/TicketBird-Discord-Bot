package org.dreamexposure.ticketbird.database

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono

interface GuildSettingsRepository: R2dbcRepository<GuildSettingsData, Long> {
    fun existsByGuildId(guildId: Long): Mono<Boolean>

    fun findByGuildId(guildId: Long): Mono<GuildSettingsData>

    @Query("""
        UPDATE guild_settings
        SET lang = :lang,
            patron_guild = :patronGuild,
            dev_guild = :devGuild,
            use_projects = :useProjects,
            auto_close_hours = :autoCloseHours,
            auto_delete_hours = :autoDeleteHours,
            requires_repair = :requiresRepair,
            awaiting_category = :awaitingCategory,
            responded_category = :respondedCategory,
            hold_category = :holdCategory,
            close_category = :closeCategory,
            support_channel = :supportChannel,
            static_message = :staticMessage,
            next_id = :nextId,
            staff = :staff,
            staff_role = :staffRole
        WHERE guild_id = :guildId
    """)
    fun updateByGuildId(
        guildId: Long,
        lang: String,
        patronGuild: Boolean,
        devGuild: Boolean,
        useProjects: Boolean,
        autoCloseHours: Int,
        autoDeleteHours: Int,
        requiresRepair: Boolean,

        awaitingCategory: Long?,
        respondedCategory: Long?,
        holdCategory: Long?,
        closeCategory: Long?,
        supportChannel: Long?,
        staticMessage: Long?,

        nextId: Int,
        staff: String,
        staffRole: Long?,
    ): Mono<Int>

    fun deleteByGuildId(guildId: Long): Mono<Void>
}
