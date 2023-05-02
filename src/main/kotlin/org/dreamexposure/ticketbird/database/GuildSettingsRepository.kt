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
            enable_logging = :enableLogging,
            auto_close_hours = :autoCloseHours,
            auto_delete_hours = :autoDeleteHours,
            requires_repair = :requiresRepair,
            awaiting_category = :awaitingCategory,
            responded_category = :respondedCategory,
            hold_category = :holdCategory,
            close_category = :closeCategory,
            support_channel = :supportChannel,
            log_channel = :logChannel,
            static_message = :staticMessage,
            next_id = :nextId,
            staff = :staff,
            staff_role = :staffRole,
            ping_option = :pingOption
        WHERE guild_id = :guildId
    """)
    fun updateByGuildId(
        guildId: Long,
        lang: String,
        patronGuild: Boolean,
        devGuild: Boolean,
        useProjects: Boolean,
        enableLogging: Boolean,
        autoCloseHours: Int,
        autoDeleteHours: Int,
        requiresRepair: Boolean,

        awaitingCategory: Long?,
        respondedCategory: Long?,
        holdCategory: Long?,
        closeCategory: Long?,
        supportChannel: Long?,
        logChannel: Long?,
        staticMessage: Long?,

        nextId: Int,
        staff: String,
        staffRole: Long?,
        pingOption: Int,
    ): Mono<Int>

    fun deleteByGuildId(guildId: Long): Mono<Void>
}
