package org.dreamexposure.ticketbird.database

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TicketRepository: R2dbcRepository<TicketData, Long> {

    fun findByGuildIdAndNumber(guildId: Long, number: Int): Mono<TicketData>

    fun findByGuildIdAndChannel(guildId: Long, channel: Long): Mono<TicketData>

    fun findByGuildId(guildId: Long): Flux<TicketData>

    fun deleteByGuildIdAndNumber(guildId: Long, number: Int): Mono<Void>

    fun deleteAllByGuildId(guildId: Long): Mono<Void>

    @Query("""
        UPDATE tickets
        SET project = :project,
            creator = :creator,
            channel = :channel,
            category = :category,
            last_activity = :lastActivity,
            transcript_sha256 = :transcriptSha256,
            attachments_sha256 = :attachmentsSha256
        WHERE guild_id = :guildId
            AND number = :number
    """)
    fun updateByGuildIdAndNumber(
        guildId: Long,
        number: Int,
        project: String,
        creator: Long,
        channel: Long,
        category: Long,
        lastActivity: Long,
        transcriptSha256: String?,
        attachmentsSha256: String?,
    ): Mono<Int>
}
